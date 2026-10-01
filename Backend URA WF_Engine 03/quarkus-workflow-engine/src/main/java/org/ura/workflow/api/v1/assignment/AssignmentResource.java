package org.ura.workflow.api.v1.assignment;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import org.ura.workflow.dto.assignment.AssignmentResponse;
import org.ura.workflow.service.AssignmentService;

@Path("/v1/assignments")
@Produces(MediaType.APPLICATION_JSON)
public class AssignmentResource {

    @Inject
    AssignmentService assignmentService;

    @GET
    @Path("/inbox/{personId}")
    public List<AssignmentResponse> inbox(
            @PathParam("personId") Long personId) {

        return assignmentService.getInbox(personId);
    }

    @GET
    @Path("/{id}")
    public AssignmentResponse get(
            @PathParam("id") Long id) {

        return assignmentService.get(id);
    }
}