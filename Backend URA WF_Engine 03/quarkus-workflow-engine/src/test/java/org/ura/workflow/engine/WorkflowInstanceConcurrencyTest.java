package org.ura.workflow.engine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.ura.workflow.entity.Person;
import org.ura.workflow.entity.WorkflowDefinition;
import org.ura.workflow.entity.WorkflowInstance;
import org.ura.workflow.repository.PersonRepository;
import org.ura.workflow.repository.WorkflowDefinitionRepository;
import org.ura.workflow.repository.WorkflowInstanceRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

/**
 * Validates the optimistic-locking mechanism WorkflowInstance.version
 * (@Version) backs — this is the safeguard the backend assessment asked
 * for under "Add optimistic concurrency control", covering the case where
 * two requests read the same instance and try to transition it at once.
 *
 * <p>Deliberately does NOT use {@code @TestTransaction}: that annotation
 * wraps a whole test in a single transaction, which would hide the very
 * conflict this test needs to produce between two genuinely separate,
 * committed transactions. Fixture rows are cleaned up manually instead.
 */
@QuarkusTest
class WorkflowInstanceConcurrencyTest {

    @Inject
    WorkflowInstanceRepository workflowInstanceRepository;

    @Inject
    WorkflowDefinitionRepository definitionRepository;

    @Inject
    PersonRepository personRepository;

    private Long instanceId;

    @AfterEach
    void cleanup() {
        if (instanceId != null) {
            QuarkusTransaction.requiringNew().run(() ->
                    workflowInstanceRepository.deleteById(instanceId));
        }
    }

    @Test
    void concurrentTransition_secondCommitLosesWithOptimisticLockException() {
        WorkflowDefinition definition = QuarkusTransaction.requiringNew().call(() ->
                definitionRepository.findActiveByKey("internal-memo").orElseThrow());
        Person officer = QuarkusTransaction.requiringNew().call(() ->
                personRepository.findByStaffNumber("DT-OFF-001").orElseThrow());

        // Transaction 1: create and commit an instance (version 0).
        instanceId = QuarkusTransaction.requiringNew().call(() -> {
            WorkflowInstance instance = new WorkflowInstance();
            instance.workflowDefinition = definition;
            instance.businessReference = "CONCURRENCY-TEST";
            instance.currentStep = 1;
            instance.status = WorkflowState.IN_PROGRESS.name();
            instance.startedBy = officer;
            instance.startedAt = LocalDateTime.now();
            workflowInstanceRepository.persist(instance);
            return instance.id;
        });

        // "Request A" reads the instance (still version 0) and holds onto it
        // without committing a change yet.
        WorkflowInstance readByRequestA = QuarkusTransaction.requiringNew().call(() ->
                workflowInstanceRepository.findById(instanceId));

        // "Request B" reads the same instance, changes it, and commits first —
        // the row is now at version 1.
        QuarkusTransaction.requiringNew().run(() -> {
            WorkflowInstance readByRequestB = workflowInstanceRepository.findById(instanceId);
            readByRequestB.currentStep = 2;
        });

        // Request A now tries to commit its own change against the stale
        // (version 0) copy it read earlier — Hibernate must reject this.
        // The failure surfaces at commit time via JTA, which can wrap the
        // underlying OptimisticLockException in a transaction-manager
        // exception (e.g. RollbackException) rather than throwing it
        // directly, so check the cause chain instead of the top-level type.
        readByRequestA.currentStep = 3;
        Exception thrown = assertThrows(Exception.class, () ->
                QuarkusTransaction.requiringNew().run(() ->
                        workflowInstanceRepository.getEntityManager().merge(readByRequestA)));

        assertTrue(containsOptimisticLockException(thrown),
                "expected an OptimisticLockException somewhere in the cause chain of: " + thrown);
    }

    private static boolean containsOptimisticLockException(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof OptimisticLockException) {
                return true;
            }
            if (cause.getCause() == cause) {
                break; // guard against self-referential cause loops
            }
        }
        return false;
    }
}
