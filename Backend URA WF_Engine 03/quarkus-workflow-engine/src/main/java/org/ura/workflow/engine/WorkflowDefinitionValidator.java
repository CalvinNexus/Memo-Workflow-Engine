package org.ura.workflow.engine;

import java.util.List;

import org.ura.workflow.engine.exception.InvalidWorkflowStateException;
import org.ura.workflow.entity.WorkflowDefinition;
import org.ura.workflow.entity.WorkflowStep;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Structural validation of a workflow definition's steps (backend
 * assessment §3, MEDIUM: "Validate workflow definition integrity"). START
 * assumes step 1 exists and each APPROVE assumes step N+1 exists or the
 * current step is final — this checks those assumptions hold for the whole
 * definition before it is used, rather than failing mid-flight on whichever
 * gap a particular instance happens to reach.
 *
 * <p>Runs on every {@code start()} call. It is intentionally cheap (a handful
 * of steps, in memory) so re-validating per start is not a performance
 * concern, and it means a bad edit to a definition's steps is caught the
 * next time anyone tries to use it rather than only when an admin UI
 * happens to run a separate check.
 */
@ApplicationScoped
public class WorkflowDefinitionValidator {

    public void validate(WorkflowDefinition definition, List<WorkflowStep> steps) {
        String key = definition.workflowKey;

        if (steps == null || steps.isEmpty()) {
            throw new InvalidWorkflowStateException(
                    "Workflow definition '" + key + "' v" + definition.version + " has no steps");
        }

        // Step numbers must be contiguous starting at 1 (1, 2, 3, ... N):
        // approve() relies on "current step + 1" to find the next step.
        for (int i = 0; i < steps.size(); i++) {
            int expected = i + 1;
            WorkflowStep step = steps.get(i);
            if (step.stepNumber == null || step.stepNumber != expected) {
                throw new InvalidWorkflowStateException(
                        "Workflow definition '" + key + "' v" + definition.version
                                + " has non-contiguous step numbers (expected " + expected
                                + " at position " + i + ")");
            }
            if (step.targetRank == null) {
                throw new InvalidWorkflowStateException(
                        "Workflow definition '" + key + "' v" + definition.version
                                + " step " + step.stepNumber + " has no target rank");
            }
            if (step.returnStep != null
                    && (step.returnStep < 1 || step.returnStep >= step.stepNumber)) {
                throw new InvalidWorkflowStateException(
                        "Workflow definition '" + key + "' v" + definition.version
                                + " step " + step.stepNumber
                                + " has an invalid return_step (" + step.returnStep
                                + "); it must point to an earlier step");
            }
        }

        long finalCount = steps.stream().filter(s -> Boolean.TRUE.equals(s.isFinal)).count();
        if (finalCount != 1) {
            throw new InvalidWorkflowStateException(
                    "Workflow definition '" + key + "' v" + definition.version
                            + " must have exactly one final step, found " + finalCount);
        }

        WorkflowStep last = steps.get(steps.size() - 1);
        if (!Boolean.TRUE.equals(last.isFinal)) {
            throw new InvalidWorkflowStateException(
                    "Workflow definition '" + key + "' v" + definition.version
                            + " must end on its final step (step " + last.stepNumber
                            + " is the last but is not marked final)");
        }
    }
}
