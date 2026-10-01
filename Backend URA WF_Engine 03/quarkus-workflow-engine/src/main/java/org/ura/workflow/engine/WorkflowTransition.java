package org.ura.workflow.engine;

import org.ura.workflow.entity.Person;

public class WorkflowTransition {

    public Long workflowInstanceId;

    public Integer previousStep;

    public Integer nextStep;

    public WorkflowState status;

    public Person assignedTo;

    public WorkflowAction action;
}