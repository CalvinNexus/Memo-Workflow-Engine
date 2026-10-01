package org.ura.workflow.dto.workflow;

import jakarta.validation.constraints.NotNull;

public class DelegateWorkflowRequest {

    @NotNull
    public Long actorId;

    @NotNull
    public Long delegateTo;

    public String comments;
}
