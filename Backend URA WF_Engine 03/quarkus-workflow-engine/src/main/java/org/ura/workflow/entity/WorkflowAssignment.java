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
@Table(name = "workflow_assignments")
public class WorkflowAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_instance_id")
    public WorkflowInstance workflowInstance;

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    public Person assignedTo;

    public LocalDateTime assignedAt;

    public LocalDateTime completedAt;

    public String status;

    public String comments;
}