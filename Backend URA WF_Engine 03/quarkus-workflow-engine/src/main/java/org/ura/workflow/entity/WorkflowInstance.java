package org.ura.workflow.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "workflow_instances")
public class WorkflowInstance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * Optimistic lock (backend assessment §3, HIGH). Two requests acting on
     * the same instance at once (e.g. two concurrent approve calls racing
     * on the same active assignment) will have the second writer fail with
     * a version conflict at commit instead of silently overwriting the
     * first — see OptimisticLockExceptionMapper for how that's surfaced
     * over the API as 409 Conflict.
     */
    @Version
    public Long version;

    @ManyToOne
    @JoinColumn(name = "workflow_definition_id")
    public WorkflowDefinition workflowDefinition;

    public String businessReference;

    public Integer currentStep;

    public String status;

    @ManyToOne
    @JoinColumn(name = "started_by")
    public Person startedBy;

    public LocalDateTime startedAt;

    public LocalDateTime completedAt;
}