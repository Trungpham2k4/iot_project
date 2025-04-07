package com.example.iot_project.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("Device")
public class Device {
    @Id
    private String deviceId;
    private String status;

    private String ledColor;

    private Double fanSpeed;

    private Boolean shouldUpdate;

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

    public Double getFanSpeed() {
        return fanSpeed;
    }

    public void setFanSpeed(Double fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    public Boolean getEditFlag() {
        return shouldUpdate;
    }

    public void setEditFlag(Boolean editFlag) {
        this.shouldUpdate = editFlag;
    }
}
