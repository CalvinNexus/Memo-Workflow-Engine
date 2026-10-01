package org.ura.workflow.repository;

import java.util.List;

import org.ura.workflow.entity.WorkflowHistory;
import org.ura.workflow.entity.WorkflowInstance;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkflowHistoryRepository implements PanacheRepository<WorkflowHistory> {

    /** Full, authoritative, time-ordered log for one instance. */
    public List<WorkflowHistory> findByInstanceOrdered(WorkflowInstance instance) {
        return list("workflowInstance = ?1 order by actionTime asc, id asc", instance);
    }

    /**
     * Who was the actor the last time this instance was at the given step?
     * Used to resolve who a RETURN should go back to.
     */
    public List<WorkflowHistory> findByInstanceAndStepOrdered(WorkflowInstance instance, Integer stepNumber) {
        return list("workflowInstance = ?1 and stepNumber = ?2 order by actionTime desc, id desc",
                instance, stepNumber);
    }
}
