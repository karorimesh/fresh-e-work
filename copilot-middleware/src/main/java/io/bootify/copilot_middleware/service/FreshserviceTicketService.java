package io.bootify.copilot_middleware.service;

import io.bootify.copilot_middleware.model.TicketFilterResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


@Service
public class FreshserviceTicketService {

    private final RestClient restClient;
    private final String freshserviceApiKey;
    private final Long freshserviceAgentId;

    public FreshserviceTicketService(
            @Value("${freshservice.base-url}") final String freshserviceBaseUrl,
            @Value("${freshservice.api-key:}") final String freshserviceApiKey,
            @Value("${freshservice.agent-id}") final Long freshserviceAgentId) {
        this.restClient = RestClient.builder()
                .baseUrl(freshserviceBaseUrl)
                .build();
        this.freshserviceApiKey = freshserviceApiKey;
        this.freshserviceAgentId = freshserviceAgentId;
    }

    public TicketFilterResponseDto fetchTicketsForConfiguredAgent() {
        if (freshserviceApiKey == null || freshserviceApiKey.isBlank()) {
            throw new IllegalStateException("freshservice.api-key must be provided.");
        }
        final String query = "\"agent_id:" + freshserviceAgentId + "\"";

        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/tickets/filter")
                        .queryParam("query", query)
                        .build())
                .headers(httpHeaders -> httpHeaders.setBasicAuth(freshserviceApiKey, "X"))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(TicketFilterResponseDto.class);
    }

}