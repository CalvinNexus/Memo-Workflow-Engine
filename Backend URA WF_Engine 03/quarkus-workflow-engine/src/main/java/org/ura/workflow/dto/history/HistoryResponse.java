package org.ura.workflow.dto.history;

import java.time.LocalDateTime;

public class HistoryResponse {

    public Long id;

    public Integer stepNumber;

    public Long actorId;

    public String action;

    public String comments;

    public LocalDateTime actionTime;
}
