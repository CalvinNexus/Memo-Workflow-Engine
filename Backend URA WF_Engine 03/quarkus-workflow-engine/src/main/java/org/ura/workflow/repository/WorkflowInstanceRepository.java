package org.ura.workflow.repository;

import org.ura.workflow.entity.WorkflowInstance;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class WorkflowInstanceRepository implements PanacheRepository<WorkflowInstance> {
}
