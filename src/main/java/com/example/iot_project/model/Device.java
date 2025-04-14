package com.example.iot_project.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document("Device")
public class Device {
    @Id
    private String deviceId;
    private String status;

    private String ledColor;

    private Integer fanSpeed;

    private Boolean shouldUpdate;

    // POJO class: MongoDB tự ánh xạ field sang đúng tên các field trong class
    @Field(name = "schedule")
    private Schedule schedule;

    @Field(name = "automation")
    private Automation automation;

    public Automation getAutomation() {
        return automation;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLedColor() {
        return ledColor;
    }

    public void setLedColor(String ledColor) {
        this.ledColor = ledColor;
    }

    public Integer getFanSpeed() {
        return fanSpeed;
    }

    public void setFanSpeed(Integer fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    public Boolean getEditFlag() {
        return shouldUpdate;
    }

    public void setEditFlag(Boolean editFlag) {
        this.shouldUpdate = editFlag;
    }
}
