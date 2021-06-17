package com.skyblockplus.api.templates;

public class Template {

    private final String success;
    private final Object object;

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
