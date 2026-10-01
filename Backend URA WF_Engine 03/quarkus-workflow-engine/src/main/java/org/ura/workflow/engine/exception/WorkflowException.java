package org.ura.workflow.engine.exception;

import jakarta.ws.rs.core.Response;

/**
 * Base type for all engine-level failures. Carries a stable machine-readable
 * code and the HTTP status the API layer should map it to, so the engine
 * itself stays framework-agnostic while the REST layer still gets a sensible
 * response (see org.ura.workflow.mapper.WorkflowExceptionMapper).
 */
public abstract class WorkflowException extends RuntimeException {

    private final String code;
    private final Response.Status status;

    protected WorkflowException(String code, Response.Status status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public Response.Status getStatus() {
        return status;
    }
}
