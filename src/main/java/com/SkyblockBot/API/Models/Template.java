package com.SkyblockBot.API.Models;

public class Template {
    private String success;
    private Object object;

    public Template(String success, Object object) {
        this.success = success;
        this.object = object;
    }

    public String getSuccess() {
        return success;
    }

    public Object getObject() {
        return object;
    }
}
