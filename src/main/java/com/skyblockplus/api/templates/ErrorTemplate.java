package com.skyblockplus.api.templates;

public class ErrorTemplate {
    private final String success;
    private final String cause;

    public ErrorTemplate(String success, String cause) {
        this.success = success;
        this.cause = cause;
    }

    public String getSuccess() {
        return success;
    }

    public String getCause() {
        return cause;
    }
}
