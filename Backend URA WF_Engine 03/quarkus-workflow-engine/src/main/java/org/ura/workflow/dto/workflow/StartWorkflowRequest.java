package org.ura.workflow.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StartWorkflowRequest {

    @NotBlank
    public String workflowKey;

    @NotBlank
    public String businessReference;

    @NotNull
    public Long startedBy;
}
