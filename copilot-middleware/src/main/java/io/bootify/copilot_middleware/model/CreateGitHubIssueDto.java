package io.bootify.copilot_middleware.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class CreateGitHubIssueDto {

    @NotBlank
    @Size(max = 256)
    private String title;

    private String body;

    private List<String> assignees;

    private Integer milestone;

    private List<String> labels;

}