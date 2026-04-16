/*
 * Copyright (c) 2026.
 * @author Patrick Mutwiri <dev@patric.xyz> on 2/24/26, 9:31 PM
 *
 */

package com.mutwiri.licensemanager.exceptions;

/**
 * Standard API error response wrapper.
 */
public class ErrorResponse {
    private String code;
    private String message;
    private int status;
    private String path;
    private String requestId;
    private long timestamp;

    public ErrorResponse(String code, String message, long timestamp) {
        this(code, message, 0, null, null, timestamp);
    }

    public ErrorResponse(String code, String message, int status, String path, String requestId, long timestamp) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.path = path;
        this.requestId = requestId;
        this.timestamp = timestamp;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
