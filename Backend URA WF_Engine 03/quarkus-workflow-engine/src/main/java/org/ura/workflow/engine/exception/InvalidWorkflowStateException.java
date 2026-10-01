package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/** The requested action cannot be performed given the instance's current status. */
public class InvalidWorkflowStateException extends WorkflowException {

    public InvalidWorkflowStateException(String message) {
        super("INVALID_WORKFLOW_STATE", Response.Status.CONFLICT, message);
    }
}
