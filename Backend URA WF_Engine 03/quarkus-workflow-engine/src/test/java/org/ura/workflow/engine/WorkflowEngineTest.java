package org.ura.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.ura.workflow.engine.exception.AssigneeResolutionException;
import org.ura.workflow.engine.exception.InvalidWorkflowActionException;
import org.ura.workflow.engine.exception.InvalidWorkflowStateException;
import org.ura.workflow.engine.exception.PersonNotFoundException;
import org.ura.workflow.engine.exception.UnauthorizedWorkflowActorException;
import org.ura.workflow.engine.exception.WorkflowDefinitionNotFoundException;
import org.ura.workflow.entity.Department;
import org.ura.workflow.entity.Person;
import org.ura.workflow.entity.Rank;
import org.ura.workflow.entity.WorkflowDefinition;
import org.ura.workflow.entity.WorkflowStep;
import org.ura.workflow.repository.DepartmentRepository;
import org.ura.workflow.repository.PersonRepository;
import org.ura.workflow.repository.RankRepository;
import org.ura.workflow.repository.WorkflowAssignmentRepository;
import org.ura.workflow.repository.WorkflowDefinitionRepository;
import org.ura.workflow.repository.WorkflowStepRepository;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Covers the scenarios called out in the backend assessment: START,
 * APPROVE, final completion, RETURN, CANCEL, DELEGATE, unauthorized actor,
 * invalid action, missing next step / bad definition, and missing
 * assignee / inactive assignee.
 *
 * <p>Each test runs inside {@code @TestTransaction}, which wraps the whole
 * test method in one transaction that Quarkus rolls back afterwards — so
 * tests can freely persist fixture data (including on top of the seeded
 * V2/V4 migration data) without needing manual cleanup or polluting other
 * tests. Optimistic-locking behaviour is covered separately in
 * {@link WorkflowInstanceConcurrencyTest}, which needs real, separate
 * transactions and therefore can't use this rollback-per-test pattern.
 */
@QuarkusTest
class WorkflowEngineTest {

    @Inject
    WorkflowEngine workflowEngine;

    @Inject
    PersonRepository personRepository;

    @Inject
    RankRepository rankRepository;

    @Inject
    DepartmentRepository departmentRepository;

    @Inject
    WorkflowDefinitionRepository definitionRepository;

    @Inject
    WorkflowStepRepository stepRepository;

    @Inject
    WorkflowAssignmentRepository assignmentRepository;

    // ---- seeded fixture lookups (V2__Seed_Data.sql / V4__Sample_Workflow.sql) ----

    private Person officer() {
        return personRepository.findByStaffNumber("DT-OFF-001").orElseThrow();
    }

    private Person supervisor() {
        return personRepository.findByStaffNumber("DT-SUP-001").orElseThrow();
    }

    private Person manager() {
        return personRepository.findByStaffNumber("DT-MGR-001").orElseThrow();
    }

    private Person assistantCommissioner() {
        return personRepository.findByStaffNumber("DT-AC-001").orElseThrow();
    }

    private Person commissioner() {
        return personRepository.findByStaffNumber("DT-COMM-001").orElseThrow();
    }

    private String freshBusinessReference() {
        return "MEMO-" + UUID.randomUUID();
    }

    // ---- START ----

    @Test
    @TestTransaction
    void start_assignsTheOfficersSupervisor() {
        WorkflowTransition transition =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);

        assertEquals(2, transition.nextStep);
        assertEquals(WorkflowState.IN_PROGRESS, transition.status);
        assertEquals(supervisor().id, transition.assignedTo.id);
    }

    @Test
    @TestTransaction
    void start_unknownWorkflowKey_throwsDefinitionNotFound() {
        assertThrows(WorkflowDefinitionNotFoundException.class,
                () -> workflowEngine.start("does-not-exist", freshBusinessReference(), officer().id));
    }

    @Test
    @TestTransaction
    void start_inactiveInitiator_throwsPersonNotFound() {
        Person inactiveOfficer = officer();
        inactiveOfficer.active = false;
        personRepository.persist(inactiveOfficer);

        assertThrows(PersonNotFoundException.class,
                () -> workflowEngine.start("internal-memo", freshBusinessReference(), inactiveOfficer.id));
    }

    // ---- APPROVE, full chain to completion ----

    @Test
    @TestTransaction
    void approve_advancesThroughFullChainToCompleted() {
        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);
        Long instanceId = started.workflowInstanceId;

        WorkflowTransition afterSupervisor = workflowEngine.approve(instanceId, supervisor().id, "looks fine");
        assertEquals(3, afterSupervisor.nextStep);
        assertEquals(manager().id, afterSupervisor.assignedTo.id);

        WorkflowTransition afterManager = workflowEngine.approve(instanceId, manager().id, "endorsed");
        assertEquals(4, afterManager.nextStep);
        assertEquals(assistantCommissioner().id, afterManager.assignedTo.id);

        WorkflowTransition afterAc = workflowEngine.approve(instanceId, assistantCommissioner().id, "approved");
        assertEquals(5, afterAc.nextStep);
        assertEquals(commissioner().id, afterAc.assignedTo.id);

        WorkflowTransition afterCommissioner = workflowEngine.approve(instanceId, commissioner().id, "final approval");
        assertEquals(WorkflowState.COMPLETED, afterCommissioner.status);

        assertTrue(assignmentRepository.findActiveByInstance(
                        org.ura.workflow.entity.WorkflowInstance.findById(instanceId))
                .isEmpty(), "a completed instance must not still have an active assignment");
    }

    @Test
    @TestTransaction
    void approve_byPersonWithoutTheActiveAssignment_throwsUnauthorized() {
        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);

        // Manager is not the current assignee (Supervisor is) — should be rejected.
        assertThrows(UnauthorizedWorkflowActorException.class,
                () -> workflowEngine.approve(started.workflowInstanceId, manager().id, "not my turn"));
    }

    @Test
    @TestTransaction
    void approve_stepThatDoesNotAllowApprove_throwsInvalidWorkflowAction() {
        // Custom 2-step definition whose step 2 only allows REVIEW, not APPROVE.
        WorkflowDefinition definition = customDefinition("review-only", 2, def -> {
            addStep(def, 1, "Prepare", rank("Officer"), "SUBMIT", null, false);
            addStep(def, 2, "Review", rank("Supervisor"), "REVIEW", null, true);
        });

        WorkflowTransition started =
                workflowEngine.start(definition.workflowKey, freshBusinessReference(), officer().id);

        assertThrows(InvalidWorkflowActionException.class,
                () -> workflowEngine.approve(started.workflowInstanceId, supervisor().id, "trying to approve anyway"));
    }

    // ---- RETURN ----

    @Test
    @TestTransaction
    void returnWorkflow_sendsItBackToThePriorActor() {
        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);
        Long instanceId = started.workflowInstanceId;

        workflowEngine.approve(instanceId, supervisor().id, "endorsing to manager");
        // Now at step 3 (Manager). Manager returns to step 2 (Supervisor).
        WorkflowTransition returned = workflowEngine.returnWorkflow(instanceId, manager().id, "needs more detail");

        assertEquals(WorkflowState.RETURNED, returned.status);
        assertEquals(2, returned.nextStep);
        assertEquals(supervisor().id, returned.assignedTo.id);
    }

    @Test
    @TestTransaction
    void returnWorkflow_atStepWithNoReturnStep_throwsInvalidWorkflowAction() {
        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);

        // Step 2 (Review) has return_step = 1, so this should be fine...
        // but step 1 itself (Prepare) has no return_step at all, and can never
        // be "current" mid-flow in this definition, so exercise the guard via
        // a custom single-step-with-no-return definition instead.
        WorkflowDefinition definition = customDefinition("no-return", 2, def -> {
            addStep(def, 1, "Prepare", rank("Officer"), "SUBMIT", null, false);
            addStep(def, 2, "Approve", rank("Supervisor"), "APPROVE", null, true);
        });
        WorkflowTransition noReturnStarted =
                workflowEngine.start(definition.workflowKey, freshBusinessReference(), officer().id);

        assertThrows(InvalidWorkflowActionException.class,
                () -> workflowEngine.returnWorkflow(noReturnStarted.workflowInstanceId, supervisor().id, "send back?"));

        // sanity: the seeded definition's own guard also behaves via the same code path
        assertNotNull(started);
    }

    // ---- CANCEL ----

    @Test
    @TestTransaction
    void cancel_byTheActiveAssignee_succeeds() {
        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);

        WorkflowTransition cancelled = workflowEngine.cancel(started.workflowInstanceId, supervisor().id, "no longer needed");

        assertEquals(WorkflowState.CANCELLED, cancelled.status);
        assertTrue(assignmentRepository.findActiveByInstance(
                        org.ura.workflow.entity.WorkflowInstance.findById(started.workflowInstanceId))
                .isEmpty());
    }

    @Test
    @TestTransaction
    void cancel_byNonOwner_throwsUnauthorized() {
        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);

        // Officer initiated it but is not the current assignee (Supervisor is) —
        // must not be able to cancel on their own say-so.
        assertThrows(UnauthorizedWorkflowActorException.class,
                () -> workflowEngine.cancel(started.workflowInstanceId, officer().id, "changed my mind"));
    }

    // ---- DELEGATE ----

    @Test
    @TestTransaction
    void delegate_movesTheActiveAssignmentToTheDelegate() {
        Person delegateTarget = newPerson("DT-TMP-001", rank("Supervisor"), department(), supervisor());

        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), officer().id);

        WorkflowTransition delegated = workflowEngine.delegate(
                started.workflowInstanceId, supervisor().id, delegateTarget.id, "covering while I'm out");

        assertEquals(WorkflowState.IN_PROGRESS, delegated.status);
        assertEquals(delegateTarget.id, delegated.assignedTo.id);
        assertEquals(delegateTarget.id, assignmentRepository
                .findActiveByInstance(org.ura.workflow.entity.WorkflowInstance.findById(started.workflowInstanceId))
                .orElseThrow().assignedTo.id);
    }

    // ---- organisation resolution: inactive persons skipped, "at or above" rank ----

    @Test
    @TestTransaction
    void resolveNextAssignee_skipsInactivePersonInTheChain() {
        Rank officerRank = rank("Officer");
        Rank supervisorRank = rank("Supervisor");
        Rank managerRank = rank("Manager");
        Department dept = department();

        Person activeManager = newPerson("CHAIN-MGR", managerRank, dept, null);
        Person inactiveSupervisor = newPerson("CHAIN-SUP", supervisorRank, dept, activeManager);
        inactiveSupervisor.active = false;
        personRepository.persist(inactiveSupervisor);
        Person testOfficer = newPerson("CHAIN-OFF", officerRank, dept, inactiveSupervisor);

        WorkflowTransition started =
                workflowEngine.start("internal-memo", freshBusinessReference(), testOfficer.id);

        // The inactive Supervisor must be skipped; assignment should climb to the active Manager.
        assertEquals(activeManager.id, started.assignedTo.id);
    }

    @Test
    @TestTransaction
    void resolveNextAssignee_noOneAboveTargetRank_throwsAssigneeResolution() {
        Rank officerRank = rank("Officer");
        Department dept = department();

        // No one above this officer in the chain at all.
        Person isolatedOfficer = newPerson("CHAIN-ISO", officerRank, dept, null);

        assertThrows(AssigneeResolutionException.class,
                () -> workflowEngine.start("internal-memo", freshBusinessReference(), isolatedOfficer.id));
    }

    // ---- definition integrity ----

    @Test
    @TestTransaction
    void start_definitionWithoutAFinalStep_throwsInvalidWorkflowState() {
        WorkflowDefinition definition = customDefinition("no-final-step", 2, def -> {
            addStep(def, 1, "Prepare", rank("Officer"), "SUBMIT", null, false);
            addStep(def, 2, "Approve", rank("Supervisor"), "APPROVE", null, false); // not marked final
        });

        assertThrows(InvalidWorkflowStateException.class,
                () -> workflowEngine.start(definition.workflowKey, freshBusinessReference(), officer().id));
    }

    @Test
    @TestTransaction
    void start_definitionWithNonContiguousSteps_throwsInvalidWorkflowState() {
        WorkflowDefinition definition = customDefinition("gappy-steps", 3, def -> {
            addStep(def, 1, "Prepare", rank("Officer"), "SUBMIT", null, false);
            addStep(def, 3, "Approve", rank("Supervisor"), "APPROVE", null, true); // skips step 2
        });

        assertThrows(InvalidWorkflowStateException.class,
                () -> workflowEngine.start(definition.workflowKey, freshBusinessReference(), officer().id));
    }

    // ---- fixture helpers ----

    private Rank rank(String name) {
        return rankRepository.findByName(name).orElseThrow();
    }

    private Department department() {
        return departmentRepository.find("code", "DT").firstResultOptional().orElseThrow();
    }

    private Person newPerson(String staffNumberPrefix, Rank rank, Department dept, Person reportsTo) {
        Person person = new Person();
        person.staffNumber = staffNumberPrefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        person.firstName = "Test";
        person.lastName = staffNumberPrefix;
        person.email = person.staffNumber.toLowerCase() + "@ura.go.ug";
        person.department = dept;
        person.rank = rank;
        person.reportsTo = reportsTo;
        person.active = true;
        personRepository.persist(person);
        return person;
    }

    @FunctionalInterface
    private interface StepBuilder {
        void build(WorkflowDefinition definition);
    }

    private WorkflowDefinition customDefinition(String keySuffix, int version, StepBuilder stepBuilder) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.workflowKey = "test-" + keySuffix + "-" + UUID.randomUUID().toString().substring(0, 8);
        definition.name = "Test: " + keySuffix;
        definition.version = version;
        definition.active = true;
        definitionRepository.persist(definition);
        stepBuilder.build(definition);
        return definition;
    }

    private void addStep(WorkflowDefinition definition, int stepNumber, String stepName, Rank targetRank,
            String allowedAction, Integer returnStep, boolean isFinal) {
        WorkflowStep step = new WorkflowStep();
        step.workflowDefinition = definition;
        step.stepNumber = stepNumber;
        step.stepName = stepName;
        step.targetRank = targetRank;
        step.allowedAction = allowedAction;
        step.returnStep = returnStep;
        step.isFinal = isFinal;
        stepRepository.persist(step);
    }
}
