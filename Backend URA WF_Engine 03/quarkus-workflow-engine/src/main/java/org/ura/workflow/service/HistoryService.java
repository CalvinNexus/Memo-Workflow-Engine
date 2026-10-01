package org.ura.workflow.service;

import java.util.List;

import org.ura.workflow.dto.history.HistoryResponse;
import org.ura.workflow.engine.exception.WorkflowNotFoundException;
import org.ura.workflow.entity.WorkflowHistory;
import org.ura.workflow.entity.WorkflowInstance;
import org.ura.workflow.repository.WorkflowHistoryRepository;
import org.ura.workflow.repository.WorkflowInstanceRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Audit History Service: append-only authoritative log, read by consumers and the Admin UI. */
@ApplicationScoped
public class HistoryService {

    @Inject
    WorkflowHistoryRepository historyRepository;

    @Inject
    WorkflowInstanceRepository workflowInstanceRepository;

    public List<HistoryResponse> getWorkflowHistory(Long workflowInstanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findByIdOptional(workflowInstanceId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowInstanceId));

        return historyRepository.findByInstanceOrdered(instance).stream()
                .map(this::toResponse)
                .toList();
    }

    private HistoryResponse toResponse(WorkflowHistory history) {
        HistoryResponse response = new HistoryResponse();
        response.id = history.id;
        response.stepNumber = history.stepNumber;
        response.actorId = history.actor != null ? history.actor.id : null;
        response.action = history.action;
        response.comments = history.comments;
        response.actionTime = history.actionTime;
        return response;
    }
}
