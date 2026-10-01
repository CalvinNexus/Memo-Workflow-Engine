package org.ura.workflow.engine;

import java.time.LocalDateTime;
import java.util.List;

import org.ura.workflow.engine.exception.AssigneeResolutionException;
import org.ura.workflow.engine.exception.InvalidWorkflowActionException;
import org.ura.workflow.engine.exception.InvalidWorkflowStateException;
import org.ura.workflow.engine.exception.PersonNotFoundException;
import org.ura.workflow.engine.exception.UnauthorizedWorkflowActorException;
import org.ura.workflow.engine.exception.WorkflowDefinitionNotFoundException;
import org.ura.workflow.engine.exception.WorkflowNotFoundException;
import org.ura.workflow.entity.Person;
import org.ura.workflow.entity.Rank;
import org.ura.workflow.entity.WorkflowAssignment;
import org.ura.workflow.entity.WorkflowDefinition;
import org.ura.workflow.entity.WorkflowHistory;
import org.ura.workflow.entity.WorkflowInstance;
import org.ura.workflow.entity.WorkflowStep;
import org.ura.workflow.repository.PersonRepository;
import org.ura.workflow.repository.WorkflowAssignmentRepository;
import org.ura.workflow.repository.WorkflowDefinitionRepository;
import org.ura.workflow.repository.WorkflowHistoryRepository;
import org.ura.workflow.repository.WorkflowInstanceRepository;
import org.ura.workflow.repository.WorkflowStepRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * The runtime engine (architecture doc: "Runtime Engine"). Creates and
 * advances workflow instances, resolves the next assignee from the
 * organisation model, enforces allowed actions for the current step and
 * actor, and writes the authoritative history. Knows businessReference only
 * as an opaque string — it never opens or validates consumer document
 * content.
 *
 * <p><b>Rank resolution rule (documented per backend assessment §3):</b>
 * {@link #resolveNextAssignee} walks the {@code reports_to} chain and stops
 * at the first person whose rank level is <em>at or above</em> the step's
 * target rank, not strictly equal to it. This is intentional: URA's
 * reporting lines are not guaranteed to include every rank for every
 * person (e.g. an Officer may report directly to a Manager if a
 * Supervisor post is vacant), and requiring an exact match would leave
 * such items unroutable. Definitions should still be authored assuming
 * each step's target rank is normally reachable by an exact hop; "at or
 * above" is the fallback that keeps the engine live when the org chart is
 * imperfect, not a way to skip levels deliberately.
 */
@ApplicationScoped
public class WorkflowEngine {

    private static final String ASSIGNMENT_ACTIVE = "ACTIVE";
    private static final String ASSIGNMENT_COMPLETED = "COMPLETED";
    private static final String ASSIGNMENT_RETURNED = "RETURNED";
    private static final String ASSIGNMENT_CANCELLED = "CANCELLED";
    private static final String ASSIGNMENT_DELEGATED = "DELEGATED";

    @Inject
    WorkflowInstanceRepository workflowInstanceRepository;

    @Inject
    WorkflowDefinitionRepository definitionRepository;

    @Inject
    WorkflowStepRepository stepRepository;

    @Inject
    WorkflowAssignmentRepository assignmentRepository;

    @Inject
    WorkflowHistoryRepository historyRepository;

    @Inject
    PersonRepository personRepository;

    @Inject
    WorkflowDefinitionValidator definitionValidator;

    /**
     * start: create instance, pin the active definition version, and hand
     * the item to whoever should act after the initiator (e.g. the Officer's
     * Supervisor for an upward memo path). If the definition is a single
     * final step, the instance completes immediately.
     */
    @Transactional
    public WorkflowTransition start(String workflowKey, String businessReference, Long startedById) {

        WorkflowDefinition definition = definitionRepository.findActiveByKey(workflowKey)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(workflowKey));

        List<WorkflowStep> steps = stepRepository.findByDefinitionOrdered(definition);
        definitionValidator.validate(definition, steps);

        Person startedBy = findActivePerson(startedById);

        WorkflowStep firstStep = steps.stream()
                .filter(s -> s.stepNumber == 1)
                .findFirst()
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(workflowKey, 1));

        WorkflowInstance instance = new WorkflowInstance();
        instance.workflowDefinition = definition;
        instance.businessReference = businessReference;
        instance.currentStep = firstStep.stepNumber;
        instance.status = WorkflowState.IN_PROGRESS.name();
        instance.startedBy = startedBy;
        instance.startedAt = LocalDateTime.now();
        workflowInstanceRepository.persist(instance);

        writeHistory(instance, startedBy, firstStep.stepNumber, WorkflowAction.START.name(), null);

        if (Boolean.TRUE.equals(firstStep.isFinal)) {
            return complete(instance, WorkflowAction.START);
        }

        return advanceTo(instance, startedBy, firstStep, WorkflowAction.START);
    }

    /** approve / advance: verify the action is allowed at this step, resolve next assignee, move step or complete. */
    @Transactional
    public WorkflowTransition approve(Long workflowInstanceId, Long actorId, String comments) {

        WorkflowInstance instance = findInstance(workflowInstanceId);
        WorkflowAssignment activeAssignment = requireInProgress(instance);
        requireActor(activeAssignment, actorId, instance.id);

        WorkflowDefinition definition = instance.workflowDefinition;
        WorkflowStep currentStep = stepRepository
                .findByDefinitionAndStepNumber(definition, instance.currentStep)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(definition.workflowKey, instance.currentStep));

        requireActionAllowed(currentStep, WorkflowAction.APPROVE);

        closeAssignment(activeAssignment, ASSIGNMENT_COMPLETED, comments);
        writeHistory(instance, activeAssignment.assignedTo, currentStep.stepNumber, WorkflowAction.APPROVE.name(), comments);

        if (Boolean.TRUE.equals(currentStep.isFinal)) {
            return complete(instance, WorkflowAction.APPROVE);
        }

        return advanceTo(instance, activeAssignment.assignedTo, currentStep, WorkflowAction.APPROVE);
    }

    /** return: send back per definition rules, record comment. */
    @Transactional
    public WorkflowTransition returnWorkflow(Long workflowInstanceId, Long actorId, String comments) {

        WorkflowInstance instance = findInstance(workflowInstanceId);
        WorkflowAssignment activeAssignment = requireInProgress(instance);
        requireActor(activeAssignment, actorId, instance.id);

        WorkflowDefinition definition = instance.workflowDefinition;
        WorkflowStep currentStep = stepRepository
                .findByDefinitionAndStepNumber(definition, instance.currentStep)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(definition.workflowKey, instance.currentStep));

        if (currentStep.returnStep == null) {
            throw new InvalidWorkflowActionException(
                    "Step " + currentStep.stepNumber + " of workflow '" + definition.workflowKey
                            + "' does not allow return");
        }

        closeAssignment(activeAssignment, ASSIGNMENT_RETURNED, comments);
        writeHistory(instance, activeAssignment.assignedTo, currentStep.stepNumber, WorkflowAction.RETURN.name(), comments);

        WorkflowStep returnStep = stepRepository
                .findByDefinitionAndStepNumber(definition, currentStep.returnStep)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(definition.workflowKey, currentStep.returnStep));

        Person returnTo = resolveReturnActor(instance, returnStep);

        createAssignment(instance, returnTo);
        instance.currentStep = returnStep.stepNumber;
        instance.status = WorkflowState.RETURNED.name();

        WorkflowTransition transition = new WorkflowTransition();
        transition.workflowInstanceId = instance.id;
        transition.previousStep = currentStep.stepNumber;
        transition.nextStep = returnStep.stepNumber;
        transition.status = WorkflowState.RETURNED;
        transition.assignedTo = returnTo;
        transition.action = WorkflowAction.RETURN;
        return transition;
    }

    /**
     * cancel: close the active assignment and end the instance. Requires
     * the caller to hold the active assignment, same as approve/return —
     * cancellation is not an administrative override in Stage 1. A
     * separate admin-cancel capability (with its own authorization model)
     * would need to be designed explicitly before allowing anyone else to
     * cancel an in-flight instance.
     */
    @Transactional
    public WorkflowTransition cancel(Long workflowInstanceId, Long actorId, String comments) {

        WorkflowInstance instance = findInstance(workflowInstanceId);
        WorkflowAssignment activeAssignment = requireInProgress(instance);
        requireActor(activeAssignment, actorId, instance.id);

        closeAssignment(activeAssignment, ASSIGNMENT_CANCELLED, comments);

        instance.status = WorkflowState.CANCELLED.name();
        instance.completedAt = LocalDateTime.now();

        writeHistory(instance, activeAssignment.assignedTo, instance.currentStep, WorkflowAction.CANCEL.name(), comments);

        WorkflowTransition transition = new WorkflowTransition();
        transition.workflowInstanceId = instance.id;
        transition.previousStep = instance.currentStep;
        transition.nextStep = instance.currentStep;
        transition.status = WorkflowState.CANCELLED;
        transition.assignedTo = null;
        transition.action = WorkflowAction.CANCEL;
        return transition;
    }

    /** delegate: hand the current step's assignment to a named colleague. */
    @Transactional
    public WorkflowTransition delegate(Long workflowInstanceId, Long actorId, Long delegateToId, String comments) {

        WorkflowInstance instance = findInstance(workflowInstanceId);
        WorkflowAssignment activeAssignment = requireInProgress(instance);
        requireActor(activeAssignment, actorId, instance.id);

        Person delegateTo = findActivePerson(delegateToId);

        closeAssignment(activeAssignment, ASSIGNMENT_DELEGATED, comments);
        writeHistory(instance, activeAssignment.assignedTo, instance.currentStep, WorkflowAction.DELEGATE.name(), comments);

        createAssignment(instance, delegateTo);
        instance.status = WorkflowState.IN_PROGRESS.name();

        WorkflowTransition transition = new WorkflowTransition();
        transition.workflowInstanceId = instance.id;
        transition.previousStep = instance.currentStep;
        transition.nextStep = instance.currentStep;
        transition.status = WorkflowState.IN_PROGRESS;
        transition.assignedTo = delegateTo;
        transition.action = WorkflowAction.DELEGATE;
        return transition;
    }

    // ---- internal helpers -------------------------------------------------

    private WorkflowTransition advanceTo(WorkflowInstance instance, Person fromPerson,
            WorkflowStep completedStep, WorkflowAction action) {

        WorkflowStep nextStep = stepRepository
                .findByDefinitionAndStepNumber(instance.workflowDefinition, completedStep.stepNumber + 1)
                .orElseThrow(() -> new WorkflowDefinitionNotFoundException(
                        instance.workflowDefinition.workflowKey, completedStep.stepNumber + 1));

        Person nextAssignee = resolveNextAssignee(fromPerson, nextStep.targetRank);

        createAssignment(instance, nextAssignee);
        instance.currentStep = nextStep.stepNumber;
        instance.status = WorkflowState.IN_PROGRESS.name();

        WorkflowTransition transition = new WorkflowTransition();
        transition.workflowInstanceId = instance.id;
        transition.previousStep = completedStep.stepNumber;
        transition.nextStep = nextStep.stepNumber;
        transition.status = WorkflowState.IN_PROGRESS;
        transition.assignedTo = nextAssignee;
        transition.action = action;
        return transition;
    }

    private WorkflowTransition complete(WorkflowInstance instance, WorkflowAction action) {
        instance.status = WorkflowState.COMPLETED.name();
        instance.completedAt = LocalDateTime.now();

        writeHistory(instance, null, instance.currentStep, "COMPLETED", null);

        WorkflowTransition transition = new WorkflowTransition();
        transition.workflowInstanceId = instance.id;
        transition.previousStep = instance.currentStep;
        transition.nextStep = instance.currentStep;
        transition.status = WorkflowState.COMPLETED;
        transition.assignedTo = null;
        transition.action = action;
        return transition;
    }

    /**
     * Resolves "who is the Supervisor / Manager / … for this person?"
     * (architecture doc: Organisation Registry) by climbing the reports_to
     * chain from fromPerson until reaching someone active who is at or
     * above the target rank's level (see class javadoc). Inactive persons
     * are skipped entirely — an inactive account can never become the next
     * assignee, even if their rank and position in the chain would
     * otherwise match.
     */
    private Person resolveNextAssignee(Person fromPerson, Rank targetRank) {
        Person candidate = fromPerson.reportsTo;
        while (candidate != null) {
            boolean active = Boolean.TRUE.equals(candidate.active);
            if (active && candidate.rank != null && candidate.rank.level != null
                    && candidate.rank.level >= targetRank.level) {
                return candidate;
            }
            candidate = candidate.reportsTo;
        }
        throw new AssigneeResolutionException(fromPerson.id, targetRank.name);
    }

    /**
     * A RETURN sends the item back to whoever last acted at the return step
     * (e.g. the same Officer who prepared it, or the same Supervisor who
     * reviewed it). Falls back to the instance's initiator for step 1.
     */
    private Person resolveReturnActor(WorkflowInstance instance, WorkflowStep returnStep) {
        if (returnStep.stepNumber == 1) {
            return instance.startedBy;
        }
        List<WorkflowHistory> stepHistory =
                historyRepository.findByInstanceAndStepOrdered(instance, returnStep.stepNumber);
        return stepHistory.stream()
                .map(h -> h.actor)
                .filter(actor -> actor != null)
                .findFirst()
                .orElseThrow(() -> new InvalidWorkflowStateException(
                        "No prior actor found at step " + returnStep.stepNumber
                                + " for workflow instance " + instance.id));
    }

    private WorkflowAssignment createAssignment(WorkflowInstance instance, Person assignee) {
        WorkflowAssignment assignment = new WorkflowAssignment();
        assignment.workflowInstance = instance;
        assignment.assignedTo = assignee;
        assignment.assignedAt = LocalDateTime.now();
        assignment.status = ASSIGNMENT_ACTIVE;
        assignmentRepository.persist(assignment);
        return assignment;
    }

    private void closeAssignment(WorkflowAssignment assignment, String status, String comments) {
        assignment.status = status;
        assignment.completedAt = LocalDateTime.now();
        assignment.comments = comments;
    }

    private void writeHistory(WorkflowInstance instance, Person actor, Integer stepNumber,
            String action, String comments) {
        WorkflowHistory history = new WorkflowHistory();
        history.workflowInstance = instance;
        history.actor = actor;
        history.stepNumber = stepNumber;
        history.action = action;
        history.comments = comments;
        history.actionTime = LocalDateTime.now();
        historyRepository.persist(history);
    }

    private WorkflowInstance findInstance(Long workflowInstanceId) {
        return workflowInstanceRepository.findByIdOptional(workflowInstanceId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowInstanceId));
    }

    private Person findActivePerson(Long personId) {
        Person person = personRepository.findByIdOptional(personId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
        if (!Boolean.TRUE.equals(person.active)) {
            throw new PersonNotFoundException(personId);
        }
        return person;
    }

    private WorkflowAssignment requireInProgress(WorkflowInstance instance) {
        if (WorkflowState.COMPLETED.name().equals(instance.status)
                || WorkflowState.CANCELLED.name().equals(instance.status)) {
            throw new InvalidWorkflowStateException(
                    "Workflow instance " + instance.id + " is already " + instance.status);
        }
        return assignmentRepository.findActiveByInstance(instance)
                .orElseThrow(() -> new InvalidWorkflowStateException(
                        "Workflow instance " + instance.id + " has no active assignment"));
    }

    private void requireActor(WorkflowAssignment assignment, Long actorId, Long workflowInstanceId) {
        if (assignment.assignedTo == null || !assignment.assignedTo.id.equals(actorId)) {
            throw new UnauthorizedWorkflowActorException(actorId, workflowInstanceId);
        }
    }

    /**
     * Central allowed-action check (backend assessment §3, HIGH). A step's
     * {@code allowedAction} says what the *assigned actor* may do to move
     * the instance forward at that step; APPROVE is only permitted when it
     * matches. RETURN and DELEGATE are checked separately (return_step
     * presence, and "any active assignee may delegate", respectively) since
     * they are not mutually exclusive with the step's primary action.
     */
    private void requireActionAllowed(WorkflowStep step, WorkflowAction action) {
        if (step.allowedAction == null || !step.allowedAction.equalsIgnoreCase(action.name())) {
            throw new InvalidWorkflowActionException(
                    "Step " + step.stepNumber + " does not permit action " + action
                            + " (configured allowed action: " + step.allowedAction + ")");
        }
    }
}
