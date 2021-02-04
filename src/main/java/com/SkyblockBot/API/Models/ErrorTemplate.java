package com.SkyblockBot.API.Models;

public class ErrorTemplate {
    private String success;
    private String cause;

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
