package org.ura.workflow.api.error;

import java.time.Instant;

public class ErrorResponse {

    public String code;

    public String message;

    public String timestamp;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = Instant.now().toString();
    }
}
