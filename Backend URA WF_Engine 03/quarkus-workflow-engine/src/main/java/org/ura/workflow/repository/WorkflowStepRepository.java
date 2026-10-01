package org.ura.workflow.repository;

import java.util.List;
import java.util.Optional;

import org.ura.workflow.entity.WorkflowDefinition;
import org.ura.workflow.entity.WorkflowStep;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkflowStepRepository implements PanacheRepository<WorkflowStep> {

    public Optional<WorkflowStep> findByDefinitionAndStepNumber(
            WorkflowDefinition definition, Integer stepNumber) {
        return find("workflowDefinition = ?1 and stepNumber = ?2", definition, stepNumber)
                .firstResultOptional();
    }

    /** All steps for a definition, ordered by step number — used to validate definition integrity. */
    public List<WorkflowStep> findByDefinitionOrdered(WorkflowDefinition definition) {
        return list("workflowDefinition = ?1 order by stepNumber asc", definition);
    }
}
