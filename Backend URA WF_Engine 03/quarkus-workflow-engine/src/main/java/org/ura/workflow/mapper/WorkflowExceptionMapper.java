package org.ura.workflow.mapper;

import org.ura.workflow.api.error.ErrorResponse;
import org.ura.workflow.engine.exception.WorkflowException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps every engine-level failure (WorkflowException and its subclasses) to
 * a JSON error body with the status the exception itself declares, so the
 * Integration API stays a stable, documented contract even on failure.
 */
@Provider
public class WorkflowExceptionMapper implements ExceptionMapper<WorkflowException> {

    @Override
    public Response toResponse(WorkflowException exception) {
        Response.Status status = exception.getStatus() != null
                ? exception.getStatus()
                : Response.Status.BAD_REQUEST;

        return Response
                .status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(exception.getCode(), exception.getMessage()))
                .build();
    }
}
