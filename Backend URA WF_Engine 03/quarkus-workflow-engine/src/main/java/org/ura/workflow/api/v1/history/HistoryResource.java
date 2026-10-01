package org.ura.workflow.api.v1.history;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

import org.ura.workflow.dto.history.HistoryResponse;
import org.ura.workflow.service.HistoryService;

@Path("/v1/history")
@Produces(MediaType.APPLICATION_JSON)
public class HistoryResource {

    @Inject
    HistoryService historyService;

    @GET
    @Path("/workflow/{workflowInstanceId}")
    public List<HistoryResponse> getWorkflowHistory(
            @PathParam("workflowInstanceId")
            Long workflowInstanceId) {

        return historyService.getWorkflowHistory(
                workflowInstanceId
        );
    }
}