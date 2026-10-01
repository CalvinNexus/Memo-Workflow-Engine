package org.ura.workflow.repository;

import java.util.List;
import java.util.Optional;

import org.ura.workflow.entity.Person;
import org.ura.workflow.entity.WorkflowAssignment;
import org.ura.workflow.entity.WorkflowInstance;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkflowAssignmentRepository implements PanacheRepository<WorkflowAssignment> {

    private static final String ACTIVE = "ACTIVE";

    /** Stage 1: at most one active assignment per in-progress instance. */
    public Optional<WorkflowAssignment> findActiveByInstance(WorkflowInstance instance) {
        return find("workflowInstance = ?1 and status = ?2", instance, ACTIVE)
                .firstResultOptional();
    }

    /** Powers the person's inbox: everything currently awaiting their action. */
    public List<WorkflowAssignment> findActiveByPerson(Person person) {
        return list("assignedTo = ?1 and status = ?2", person, ACTIVE);
    }
}
