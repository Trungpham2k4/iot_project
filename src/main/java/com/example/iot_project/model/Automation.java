package com.example.iot_project.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@Setter
@Getter
@Builder
public class Automation {
    private String data;
    private String condition;
    private String value;

    @Field(name="do")
    private String task;
    private String device;
    private String deviceValue;
}
