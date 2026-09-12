package io.bootify.copilot_middleware.service;

import io.bootify.copilot_middleware.model.AmbiguousTaskListResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class AmbiguousTaskService {

    private final RestClient restClient;
    private final String ambiguousApiToken;
    private final Long ambiguousAssigneeId;

    public AmbiguousTaskService(
            @Value("${ambiguous.base-url:https://app.ambiguous.ai}") final String ambiguousBaseUrl,
            @Value("${ambiguous.api-token:}") final String ambiguousApiToken,
            @Value("${ambiguous.assignee-id:}") final Long ambiguousAssigneeId) {
        this.restClient = RestClient.builder()
                .baseUrl(ambiguousBaseUrl)
                .build();
        this.ambiguousApiToken = ambiguousApiToken;
        this.ambiguousAssigneeId = ambiguousAssigneeId;
    }

    public AmbiguousTaskListResponseDto fetchTasksForAssignee() {
        if (ambiguousApiToken == null || ambiguousApiToken.isBlank()) {
            throw new IllegalStateException("ambiguous.api-token must be provided.");
        }
        if (ambiguousAssigneeId == null) {
            throw new IllegalStateException("ambiguous.assignee-id must be provided.");
        }

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/tasks")
                        .queryParam("assignee_id", ambiguousAssigneeId)
                        .build())
                .header("Authorization", "Bearer " + ambiguousApiToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(AmbiguousTaskListResponseDto.class);
    }

}
