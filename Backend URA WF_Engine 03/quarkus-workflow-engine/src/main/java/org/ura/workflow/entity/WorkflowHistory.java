package org.ura.workflow.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workflow_history")
public class WorkflowHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id")
    public WorkflowInstance workflowInstance;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    public Person actor;

    public Integer stepNumber;

    public String action;

    public String comments;

    public LocalDateTime actionTime;
}