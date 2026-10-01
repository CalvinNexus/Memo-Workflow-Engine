package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/** No workflow instance exists for the given id. */
public class WorkflowNotFoundException extends WorkflowException {

    public WorkflowNotFoundException(Long workflowInstanceId) {
        super("WORKFLOW_NOT_FOUND", Response.Status.NOT_FOUND,
                "No workflow instance found with id " + workflowInstanceId);
    }
}
