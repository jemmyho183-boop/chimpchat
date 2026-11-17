package com.chimpchat.api.automation.business;

import org.springframework.http.HttpStatus;

public class InstagramCommentAutomationException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public InstagramCommentAutomationException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
