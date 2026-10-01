package org.ura.workflow.repository;

import java.util.Optional;

import org.ura.workflow.entity.WorkflowDefinition;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkflowDefinitionRepository implements PanacheRepository<WorkflowDefinition> {

    /**
     * The definition instances should pin when a new workflow starts:
     * the currently active version for a given workflow key.
     */
    public Optional<WorkflowDefinition> findActiveByKey(String workflowKey) {
        return find("workflowKey = ?1 and active = true", workflowKey)
                .firstResultOptional();
    }
}
