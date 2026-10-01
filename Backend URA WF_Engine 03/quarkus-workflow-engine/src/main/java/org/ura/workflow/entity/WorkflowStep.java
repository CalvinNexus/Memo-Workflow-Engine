package org.ura.workflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_steps")
public class WorkflowStep extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_definition_id")
    public WorkflowDefinition workflowDefinition;

    @Column(name = "step_number", nullable = false)
    public Integer stepNumber;

    @Column(name = "step_name")
    public String stepName;

    @ManyToOne
    @JoinColumn(name = "target_rank_id")
    public Rank targetRank;

    @Column(name = "allowed_action")
    public String allowedAction;

    @Column(name = "return_step")
    public Integer returnStep;

    @Column(name = "is_final", nullable = false)
    public Boolean isFinal;
}