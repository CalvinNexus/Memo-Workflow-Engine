package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/** The requested action is not allowed at the instance's current step (e.g. return with no return_step). */
public class InvalidWorkflowActionException extends WorkflowException {

    public InvalidWorkflowActionException(String message) {
        super("INVALID_WORKFLOW_ACTION", Response.Status.BAD_REQUEST, message);
    }
}
