package org.ura.workflow.service;

import java.util.List;

import org.ura.workflow.dto.assignment.AssignmentResponse;
import org.ura.workflow.engine.exception.PersonNotFoundException;
import org.ura.workflow.entity.Person;
import org.ura.workflow.entity.WorkflowAssignment;
import org.ura.workflow.repository.PersonRepository;
import org.ura.workflow.repository.WorkflowAssignmentRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

/** Assignment / Inbox Service: tracks who must act now, and exposes pending work. */
@ApplicationScoped
public class AssignmentService {

    @Inject
    WorkflowAssignmentRepository assignmentRepository;

    @Inject
    PersonRepository personRepository;

    public List<AssignmentResponse> getInbox(Long personId) {
        Person person = personRepository.findByIdOptional(personId)
                .orElseThrow(() -> new PersonNotFoundException(personId));

        return assignmentRepository.findActiveByPerson(person).stream()
                .map(this::toResponse)
                .toList();
    }

    public AssignmentResponse get(Long assignmentId) {
        WorkflowAssignment assignment = assignmentRepository.findByIdOptional(assignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "No workflow assignment found with id " + assignmentId));
        return toResponse(assignment);
    }

    private AssignmentResponse toResponse(WorkflowAssignment assignment) {
        AssignmentResponse response = new AssignmentResponse();
        response.assignmentId = assignment.id;
        response.workflowInstanceId = assignment.workflowInstance.id;
        response.workflowKey = assignment.workflowInstance.workflowDefinition.workflowKey;
        response.businessReference = assignment.workflowInstance.businessReference;
        response.stepNumber = assignment.workflowInstance.currentStep;
        response.status = assignment.status;
        response.assignedAt = assignment.assignedAt;
        response.completedAt = assignment.completedAt;
        return response;
    }
}
