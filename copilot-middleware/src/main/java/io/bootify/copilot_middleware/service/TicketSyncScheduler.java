package io.bootify.copilot_middleware.service;

import io.bootify.copilot_middleware.model.AmbiguousTaskDto;
import io.bootify.copilot_middleware.model.AmbiguousTaskListResponseDto;
import io.bootify.copilot_middleware.model.CreateGitHubIssueDto;
import io.bootify.copilot_middleware.model.TicketDto;
import io.bootify.copilot_middleware.model.TicketFilterResponseDto;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class TicketSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(TicketSyncScheduler.class);

    private final FreshserviceTicketService freshserviceTicketService;
    private final AmbiguousTaskService ambiguousTaskService;
    private final GitHubIssueService gitHubIssueService;
    private final String githubOwner;
    private final String githubRepo;
    private final Set<Long> processedTicketIds = new LinkedHashSet<>();
    private final Set<Long> processedTaskIds = new LinkedHashSet<>();

    public TicketSyncScheduler(
            final FreshserviceTicketService freshserviceTicketService,
            final AmbiguousTaskService ambiguousTaskService,
            final GitHubIssueService gitHubIssueService,
            @Value("${github.owner:}") final String githubOwner,
            @Value("${github.repo:}") final String githubRepo) {
        this.freshserviceTicketService = freshserviceTicketService;
        this.ambiguousTaskService = ambiguousTaskService;
        this.gitHubIssueService = gitHubIssueService;
        this.githubOwner = githubOwner;
        this.githubRepo = githubRepo;
    }

    @Scheduled(cron = "${ticket-sync.cron:0 */5 * * * *}")
    public void syncTicketsToGitHub() {
        if (githubOwner == null || githubOwner.isBlank() || githubRepo == null || githubRepo.isBlank()) {
            throw new IllegalStateException("github.owner and github.repo must be configured.");
        }

        syncFreshserviceTickets();
        syncAmbiguousTasks();
    }

    private void syncFreshserviceTickets() {
        final TicketFilterResponseDto response = freshserviceTicketService.fetchTicketsForConfiguredAgent();
        if (response == null || response.getTickets() == null || response.getTickets().isEmpty()) {
            log.info("No Freshservice tickets were returned for sync.");
            return;
        }

        for (final TicketDto ticket : response.getTickets()) {
            if (ticket == null || ticket.getId() == null) {
                continue;
            }
            if (!processedTicketIds.add(ticket.getId())) {
                log.info("Skipping already processed Freshservice ticket {}.", ticket.getId());
                continue;
            }

            try {
                final CreateGitHubIssueDto issuePayload = gitHubIssueService.enrichTicketForGitHubIssue(ticket);
                gitHubIssueService.createIssue(githubOwner, githubRepo, issuePayload);
                log.info("Created GitHub issue for Freshservice ticket {}.", ticket.getId());
            } catch (final RuntimeException e) {
                processedTicketIds.remove(ticket.getId());
                log.error("Failed to sync Freshservice ticket {} to GitHub.", ticket.getId(), e);
            }
        }
    }

    private void syncAmbiguousTasks() {
        final AmbiguousTaskListResponseDto response = ambiguousTaskService.fetchTasksForAssignee();
        if (response == null || response.getTasks() == null || response.getTasks().isEmpty()) {
            log.info("No Ambiguous tasks were returned for sync.");
            return;
        }

        for (final AmbiguousTaskDto task : response.getTasks()) {
            if (task == null || task.getId() == null) {
                continue;
            }
            if (!processedTaskIds.add(task.getId())) {
                log.info("Skipping already processed Ambiguous task {}.", task.getId());
                continue;
            }

            try {
                final CreateGitHubIssueDto issuePayload = buildIssueFromTask(task);
                gitHubIssueService.createIssue(githubOwner, githubRepo, issuePayload);
                log.info("Created GitHub issue for Ambiguous task {}.", task.getId());
            } catch (final RuntimeException e) {
                processedTaskIds.remove(task.getId());
                log.error("Failed to sync Ambiguous task {} to GitHub.", task.getId(), e);
            }
        }
    }

    private CreateGitHubIssueDto buildIssueFromTask(final AmbiguousTaskDto task) {
        final CreateGitHubIssueDto dto = new CreateGitHubIssueDto();
        dto.setTitle(task.getTitle() == null || task.getTitle().isBlank()
                ? "Ambiguous task" : task.getTitle());
        final StringBuilder body = new StringBuilder();
        body.append("## Ambiguous task\n");
        body.append("- Task ID: ").append(task.getId()).append("\n");
        body.append("- Assignee ID: ").append(task.getAssigneeId()).append("\n");
        body.append("- Status: ").append(task.getStatus()).append("\n");
        body.append("- Priority: ").append(task.getPriority()).append("\n");
        body.append("- Created At: ").append(task.getCreatedAt()).append("\n");
        body.append("- Updated At: ").append(task.getUpdatedAt()).append("\n");
        body.append("\n## Description\n");
        body.append(task.getDescription() == null || task.getDescription().isBlank()
                ? "No task description provided."
                : task.getDescription());
        dto.setBody(body.toString());
        dto.setLabels(List.of("bug"));
        dto.setAssignees(List.of("copilot"));
        return dto;
    }

}
