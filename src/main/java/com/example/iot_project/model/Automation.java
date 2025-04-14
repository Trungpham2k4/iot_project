package com.example.iot_project.model;

import org.springframework.data.mongodb.core.mapping.Field;

public class Automation {
    private String data;
    private String condition;
    private String value;

    @Field(name="do")
    private String task;
    private String device;
    private String deviceValue;

    public String getData() {
        return data;
    }

    public String getCondition() {
        return condition;
    }

    public String getValue() {
        return value;
    }

    public String getTask() {
        return task;
    }

    public String getDevice() {
        return device;
    }

    public String getDeviceValue() {
        return deviceValue;
    }
}
