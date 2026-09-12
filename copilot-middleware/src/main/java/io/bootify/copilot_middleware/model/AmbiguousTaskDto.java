package io.bootify.copilot_middleware.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class AmbiguousTaskDto {

    private Long id;

    private String title;

    private String description;

    @JsonProperty("assignee_id")
    private Long assigneeId;

    private String status;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    private String priority;

    @JsonProperty("project_id")
    private Long projectId;

    private String url;

}
