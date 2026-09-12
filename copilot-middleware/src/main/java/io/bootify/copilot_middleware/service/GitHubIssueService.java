package io.bootify.copilot_middleware.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootify.copilot_middleware.model.CreateGitHubIssueDto;
import io.bootify.copilot_middleware.model.GitHubIssueResponseDto;
import io.bootify.copilot_middleware.model.TicketDto;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class GitHubIssueService {

    private static final List<String> DEFAULT_ASSIGNEES = List.of("copilot");
    private static final List<String> DEFAULT_LABELS = List.of("bug");

    private final RestClient restClient;
    private final String githubApiToken;
    private final String githubApiVersion;
    private final RestClient azureAiRestClient;
    private final String azureAiApiKey;
    private final String azureAiDeployment;
    private final String azureAiApiVersion;
    private final ObjectMapper objectMapper;

    public GitHubIssueService(
            @Value("${github.base-url}") final String githubBaseUrl,
            @Value("${github.api-token:}") final String githubApiToken,
            @Value("${github.api-version}") final String githubApiVersion,
            @Value("${azure.ai.endpoint:}") final String azureAiEndpoint,
            @Value("${azure.ai.api-key:}") final String azureAiApiKey,
            @Value("${azure.ai.deployment:}") final String azureAiDeployment,
            @Value("${azure.ai.api-version:2024-02-01}") final String azureAiApiVersion) {
        this.restClient = RestClient.builder()
                .baseUrl(githubBaseUrl)
                .build();
        this.githubApiToken = githubApiToken;
        this.githubApiVersion = githubApiVersion;
        this.azureAiRestClient = RestClient.builder()
                .baseUrl(azureAiEndpoint)
                .build();
        this.azureAiApiKey = azureAiApiKey;
        this.azureAiDeployment = azureAiDeployment;
        this.azureAiApiVersion = azureAiApiVersion;
        this.objectMapper = new ObjectMapper();
    }

    public GitHubIssueResponseDto createIssue(final String owner, final String repo,
            final CreateGitHubIssueDto createGitHubIssueDto) {
        if (githubApiToken == null || githubApiToken.isBlank()) {
            throw new IllegalStateException("github.api-token must be provided.");
        }

        final CreateGitHubIssueDto payload = new CreateGitHubIssueDto();
        payload.setTitle(createGitHubIssueDto.getTitle());
        payload.setBody(createGitHubIssueDto.getBody());
        payload.setMilestone(createGitHubIssueDto.getMilestone());
        payload.setLabels(createGitHubIssueDto.getLabels());
        payload.setAssignees(resolveAssignees(createGitHubIssueDto.getAssignees()));

        final GitHubIssueResponseDto response = restClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/repos/{owner}/{repo}/issues")
                        .build(owner, repo))
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(githubApiToken);
                    httpHeaders.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
                    httpHeaders.add("X-GitHub-Api-Version", githubApiVersion);
                })
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(GitHubIssueResponseDto.class);

        if (response == null) {
            throw new IllegalStateException("GitHub returned an empty response body.");
        }
        return response;
    }

    public CreateGitHubIssueDto enrichTicketForGitHubIssue(final TicketDto ticketDto) {
        if (ticketDto == null) {
            throw new IllegalArgumentException("ticketDto must not be null.");
        }
        if (azureAiRestClient == null || azureAiApiKey == null || azureAiApiKey.isBlank()) {
            throw new IllegalStateException("azure.ai.endpoint, azure.ai.api-key and azure.ai.deployment must be provided.");
        }
        if (azureAiDeployment == null || azureAiDeployment.isBlank()) {
            throw new IllegalStateException("azure.ai.deployment must be provided.");
        }

        final String prompt = buildTicketPrompt(ticketDto);
        final Map<String, Object> requestPayload = new LinkedHashMap<>();
        requestPayload.put("messages", List.of(
                Map.of(
                        "role", "system",
                        "content", "You are a support triage assistant. Turn the ticket details into a concise GitHub issue summary and body. Return only a JSON object with keys title, body, and labels."),
                Map.of(
                        "role", "user",
                        "content", prompt)));
        requestPayload.put("temperature", 0.2);
        requestPayload.put("response_format", Map.of("type", "json_object"));

        final Map<String, Object> responsePayload = azureAiRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/openai/deployments/{deployment}/chat/completions")
                        .queryParam("api-version", azureAiApiVersion)
                        .build(azureAiDeployment))
                .header("api-key", azureAiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

        if (responsePayload == null || responsePayload.isEmpty()) {
            throw new IllegalStateException("Azure AI returned an empty response body.");
        }

        final String content = extractMessageContent(responsePayload);
        final Map<String, Object> issueMap;
        try {
            issueMap = objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Azure AI response payload.", e);
        }

        final CreateGitHubIssueDto issueDto = new CreateGitHubIssueDto();
        issueDto.setTitle(asString(issueMap.get("title"), "Fix: " + ticketDto.getSubject()));
        issueDto.setBody(asString(issueMap.get("body"), buildTicketFallbackBody(ticketDto)));
        issueDto.setLabels(asStringList(issueMap.get("labels"), DEFAULT_LABELS));
        issueDto.setAssignees(DEFAULT_ASSIGNEES);
        return issueDto;
    }

    private String buildTicketPrompt(final TicketDto ticketDto) {
        final StringBuilder prompt = new StringBuilder();
        prompt.append("Create a GitHub issue for this ticket. Use ticket facts only.\n\n");
        prompt.append("Ticket details:\n");
        prompt.append("- subject: ").append(ticketDto.getSubject()).append("\n");
        prompt.append("- status: ").append(ticketDto.getStatus()).append("\n");
        prompt.append("- priority: ").append(ticketDto.getPriority()).append("\n");
        prompt.append("- category: ").append(ticketDto.getCategory()).append("\n");
        prompt.append("- requester_id: ").append(ticketDto.getRequesterId()).append("\n");
        prompt.append("- responder_id: ").append(ticketDto.getResponderId()).append("\n");
        prompt.append("- description: ").append(ticketDto.getDescription()).append("\n");
        prompt.append("- created_at: ").append(ticketDto.getCreatedAt()).append("\n");
        prompt.append("- updated_at: ").append(ticketDto.getUpdatedAt()).append("\n");
        if (ticketDto.getCustomFields() != null && !ticketDto.getCustomFields().isEmpty()) {
            prompt.append("- custom_fields: ").append(ticketDto.getCustomFields()).append("\n");
        }
        prompt.append("\nProduce a concise GitHub issue with a short title, a detailed body, and labels [\"bug\"].");
        return prompt.toString();
    }

    private String buildTicketFallbackBody(final TicketDto ticketDto) {
        return "## Issue\n"
                + "Ticket subject: " + ticketDto.getSubject() + "\n\n"
                + "## Problem\n"
                + (ticketDto.getDescription() != null ? ticketDto.getDescription() : "No description provided.") + "\n\n"
                + "## Expected behavior\n"
                + "The issue should be triaged and resolved using the service-date contract and the current documented operational rules.\n\n"
                + "## Impact\n"
                + "Ticket status: " + ticketDto.getStatus() + "; priority: " + ticketDto.getPriority() + ".";
    }

    private String extractMessageContent(final Map<String, Object> responsePayload) {
        final List<Map<String, Object>> choices = (List<Map<String, Object>>) responsePayload.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new IllegalStateException("Azure AI response did not include any choices.");
        }
        final Map<String, Object> firstChoice = choices.get(0);
        final Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
        if (message == null || message.get("content") == null) {
            throw new IllegalStateException("Azure AI response did not include a message content payload.");
        }
        return String.valueOf(message.get("content"));
    }

    private List<String> resolveAssignees(final List<String> assignees) {
        return assignees == null || assignees.isEmpty() ? DEFAULT_ASSIGNEES : assignees;
    }

    private String asString(final Object value, final String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private List<String> asStringList(final Object value, final List<String> defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof List<?> list) {
            final List<String> strings = new ArrayList<>();
            for (final Object item : list) {
                if (item != null) {
                    strings.add(String.valueOf(item));
                }
            }
            return strings.isEmpty() ? defaultValue : strings;
        }
        return defaultValue;
    }

}