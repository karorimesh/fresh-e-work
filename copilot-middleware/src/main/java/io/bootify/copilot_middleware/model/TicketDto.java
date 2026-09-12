package io.bootify.copilot_middleware.model;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class TicketDto {

    private String subject;

    @JsonProperty("group_id")
    private Long groupId;

    @JsonProperty("department_id")
    private Long departmentId;

    private String category;

    @JsonProperty("sub_category")
    private String subCategory;

    @JsonProperty("item_category")
    private String itemCategory;

    @JsonProperty("requester_id")
    private Long requesterId;

    @JsonProperty("responder_id")
    private Long responderId;

    @JsonProperty("due_by")
    private String dueBy;

    @JsonProperty("fr_escalated")
    private Boolean frEscalated;

    private Boolean deleted;

    private Boolean spam;

    @JsonProperty("email_config_id")
    private Long emailConfigId;

    @JsonProperty("fwd_emails")
    private List<String> fwdEmails;

    @JsonProperty("reply_cc_emails")
    private List<String> replyCcEmails;

    @JsonProperty("cc_emails")
    private List<String> ccEmails;

    @JsonProperty("is_escalated")
    private Boolean isEscalated;

    @JsonProperty("fr_due_by")
    private String frDueBy;

    private Integer priority;

    private Integer source;

    private Integer status;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("to_emails")
    private List<String> toEmails;

    private Long id;

    private String type;

    private String description;

    @JsonProperty("description_text")
    private String descriptionText;

    @JsonProperty("custom_fields")
    private Map<String, Object> customFields;

    @JsonProperty("workspace_id")
    private Long workspaceId;

}
