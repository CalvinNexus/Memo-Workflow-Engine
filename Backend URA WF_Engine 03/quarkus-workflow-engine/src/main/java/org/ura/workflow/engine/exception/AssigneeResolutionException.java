package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/**
 * The organisation model (reports_to chain) could not resolve anyone at or
 * above the target rank for the next step. Signals a gap in the seeded /
 * admin-managed org data rather than a caller error.
 */
public class AssigneeResolutionException extends WorkflowException {

    public AssigneeResolutionException(Long fromPersonId, String targetRankName) {
        super("ASSIGNEE_RESOLUTION_FAILED", Response.Status.BAD_REQUEST,
                "Could not resolve a person at or above rank '" + targetRankName
                        + "' above person " + fromPersonId + " in the organisation model");
    }
}
