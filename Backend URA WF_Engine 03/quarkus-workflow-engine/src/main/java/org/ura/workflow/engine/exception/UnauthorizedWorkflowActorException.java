package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/** The acting person is not the holder of the current active assignment. */
public class UnauthorizedWorkflowActorException extends WorkflowException {

    public UnauthorizedWorkflowActorException(Long actorId, Long workflowInstanceId) {
        super("UNAUTHORIZED_WORKFLOW_ACTOR", Response.Status.FORBIDDEN,
                "Person " + actorId + " does not hold the active assignment on workflow instance "
                        + workflowInstanceId);
    }
}
