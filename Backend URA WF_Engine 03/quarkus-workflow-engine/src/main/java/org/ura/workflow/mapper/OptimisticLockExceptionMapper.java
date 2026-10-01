package org.ura.workflow.mapper;

import org.ura.workflow.api.error.ErrorResponse;

import jakarta.persistence.OptimisticLockException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps Hibernate's optimistic-lock conflict (thrown when two requests race
 * on the same @Version-ed WorkflowInstance) to 409 Conflict, mirroring how
 * WorkflowExceptionMapper reports engine-level conflicts. The caller should
 * re-fetch the instance and retry.
 */
@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException exception) {
        return Response
                .status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorResponse(
                        "CONCURRENT_MODIFICATION",
                        "This workflow instance was modified concurrently by another request; please retry."))
                .build();
    }
}
