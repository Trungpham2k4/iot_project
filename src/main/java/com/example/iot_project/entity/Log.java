package com.example.iot_project.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("Log")
public class Log {
    @Id
    private String LogId;

    private String LEDStatus;
    private String LEDColor;
    private Integer fanSpeed;

    private LocalDateTime timestamp;

    public String getLogId() {
        return LogId;
    }

    public void setLogId(String logId) {
        LogId = logId;
    }

    public String getLEDStatus() {
        return LEDStatus;
    }

    public void setLEDStatus(String LEDStatus) {
        this.LEDStatus = LEDStatus;
    }

    public String getLEDColor() {
        return LEDColor;
    }

    public void setLEDColor(String LEDColor) {
        this.LEDColor = LEDColor;
    }

    public Integer getFanSpeed() {
        return fanSpeed;
    }

    public void setFanSpeed(Integer fanSpeed) {
        this.fanSpeed = fanSpeed;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
