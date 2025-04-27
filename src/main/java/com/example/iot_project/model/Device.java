package com.example.iot_project.model;


import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Getter
@Setter
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
    private List<Schedule> schedule;

    @Field(name = "automation")
    private List<Automation> automation;

    public Boolean getEditFlag() {
        return shouldUpdate;
    }

    public void setEditFlag(Boolean editFlag) {
        this.shouldUpdate = editFlag;
    }
}
