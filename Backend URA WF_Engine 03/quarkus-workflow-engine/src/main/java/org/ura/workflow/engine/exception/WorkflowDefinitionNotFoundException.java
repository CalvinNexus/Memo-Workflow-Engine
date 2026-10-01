package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/** No active workflow definition exists for the given key, or a required step is missing. */
public class WorkflowDefinitionNotFoundException extends WorkflowException {

    public WorkflowDefinitionNotFoundException(String workflowKey) {
        super("WORKFLOW_DEFINITION_NOT_FOUND", Response.Status.NOT_FOUND,
                "No active workflow definition found for key '" + workflowKey + "'");
    }

    public WorkflowDefinitionNotFoundException(String workflowKey, Integer stepNumber) {
        super("WORKFLOW_DEFINITION_NOT_FOUND", Response.Status.NOT_FOUND,
                "Workflow definition '" + workflowKey + "' has no step " + stepNumber);
    }
}
