package org.ura.workflow.dto.workflow;

import jakarta.validation.constraints.NotNull;

public class WorkflowActionRequest {

    @NotNull
    public Long actorId;

    public String comments;
}
