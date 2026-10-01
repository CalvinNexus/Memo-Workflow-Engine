package org.ura.workflow.dto.assignment;

import java.time.LocalDateTime;

public class AssignmentResponse {

    public Long assignmentId;

    public Long workflowInstanceId;

    public String workflowKey;

    public String businessReference;

    public Integer stepNumber;

    public String status;

    public LocalDateTime assignedAt;

    public LocalDateTime completedAt;
}
