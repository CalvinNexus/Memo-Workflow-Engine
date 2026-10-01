package org.ura.workflow.api.v1.workflow;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.ura.workflow.dto.workflow.*;
import org.ura.workflow.service.WorkflowService;

@Path("/v1/workflows")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowResource {

    @Inject
    WorkflowService workflowService;

    @POST
    @Path("/start")
    public Response start(
            @Valid StartWorkflowRequest request) {

        WorkflowResponse response =
                workflowService.start(request);

        return Response
                .status(Response.Status.CREATED)
                .entity(response)
                .build();
    }

    @GET
    @Path("/{id}")
    public Response get(
            @PathParam("id") Long id) {

        return Response.ok(
                workflowService.get(id)
        ).build();
    }

    @POST
    @Path("/{id}/approve")
    public Response approve(
            @PathParam("id") Long id,
            @Valid WorkflowActionRequest request) {

        return Response.ok(
                workflowService.approve(
                        id,
                        request
                )
        ).build();
    }

    @POST
    @Path("/{id}/return")
    public Response returnWorkflow(
            @PathParam("id") Long id,
            @Valid WorkflowActionRequest request) {

        return Response.ok(
                workflowService.returnWorkflow(
                        id,
                        request
                )
        ).build();
    }

    @POST
    @Path("/{id}/cancel")
    public Response cancel(
            @PathParam("id") Long id,
            @Valid WorkflowActionRequest request) {

        return Response.ok(
                workflowService.cancel(
                        id,
                        request
                )
        ).build();
    }

    @POST
    @Path("/{id}/delegate")
    public Response delegate(
            @PathParam("id") Long id,
            @Valid DelegateWorkflowRequest request) {

        return Response.ok(
                workflowService.delegate(
                        id,
                        request
                )
        ).build();
    }
}