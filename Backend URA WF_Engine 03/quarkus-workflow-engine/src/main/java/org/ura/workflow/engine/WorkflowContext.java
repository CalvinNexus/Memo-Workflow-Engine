package org.ura.workflow.engine;

import org.ura.workflow.entity.Person;
import org.ura.workflow.entity.WorkflowAssignment;
import org.ura.workflow.entity.WorkflowDefinition;
import org.ura.workflow.entity.WorkflowInstance;
import org.ura.workflow.entity.WorkflowStep;

public class WorkflowContext {

    public WorkflowInstance instance;

    public WorkflowDefinition definition;

    public WorkflowStep currentStep;

    public Person actor;

    public WorkflowAssignment currentAssignment;
}