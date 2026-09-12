package io.bootify.copilot_middleware.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GitHubIssueResponseDto {

    private Long id;

    @JsonProperty("node_id")
    private String nodeId;

    private String url;

}