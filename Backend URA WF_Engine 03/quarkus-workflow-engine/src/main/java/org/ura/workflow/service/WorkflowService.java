package org.ura.workflow.service;

import org.ura.workflow.dto.workflow.DelegateWorkflowRequest;
import org.ura.workflow.dto.workflow.StartWorkflowRequest;
import org.ura.workflow.dto.workflow.WorkflowActionRequest;
import org.ura.workflow.dto.workflow.WorkflowResponse;
import org.ura.workflow.engine.WorkflowEngine;
import org.ura.workflow.engine.exception.WorkflowNotFoundException;
import org.ura.workflow.entity.WorkflowAssignment;
import org.ura.workflow.entity.WorkflowInstance;
import org.ura.workflow.repository.WorkflowAssignmentRepository;
import org.ura.workflow.repository.WorkflowInstanceRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WorkflowService {

    @Inject
    WorkflowEngine workflowEngine;

    @Inject
    WorkflowInstanceRepository workflowInstanceRepository;

    @Inject
    WorkflowAssignmentRepository assignmentRepository;

    public WorkflowResponse start(StartWorkflowRequest request) {
        var transition = workflowEngine.start(
                request.workflowKey, request.businessReference, request.startedBy);
        return toResponse(transition.workflowInstanceId);
    }

    public WorkflowResponse get(Long workflowInstanceId) {
        return toResponse(workflowInstanceId);
    }

    public WorkflowResponse approve(Long workflowInstanceId, WorkflowActionRequest request) {
        workflowEngine.approve(workflowInstanceId, request.actorId, request.comments);
        return toResponse(workflowInstanceId);
    }

    public WorkflowResponse returnWorkflow(Long workflowInstanceId, WorkflowActionRequest request) {
        workflowEngine.returnWorkflow(workflowInstanceId, request.actorId, request.comments);
        return toResponse(workflowInstanceId);
    }

    public WorkflowResponse cancel(Long workflowInstanceId, WorkflowActionRequest request) {
        workflowEngine.cancel(workflowInstanceId, request.actorId, request.comments);
        return toResponse(workflowInstanceId);
    }

    public WorkflowResponse delegate(Long workflowInstanceId, DelegateWorkflowRequest request) {
        workflowEngine.delegate(workflowInstanceId, request.actorId, request.delegateTo, request.comments);
        return toResponse(workflowInstanceId);
    }

    private WorkflowResponse toResponse(Long workflowInstanceId) {
        WorkflowInstance instance = workflowInstanceRepository.findByIdOptional(workflowInstanceId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowInstanceId));

        WorkflowAssignment active = assignmentRepository.findActiveByInstance(instance).orElse(null);

        WorkflowResponse response = new WorkflowResponse();
        response.workflowInstanceId = instance.id;
        response.workflowKey = instance.workflowDefinition.workflowKey;
        response.businessReference = instance.businessReference;
        response.currentStep = instance.currentStep;
        response.status = instance.status;
        response.assignedTo = active != null ? active.assignedTo.id : null;
        return response;
    }
}
