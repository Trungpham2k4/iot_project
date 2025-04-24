package com.example.iot_project.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Arrays;

@Getter
@Setter
@Builder
public class Schedule{

    @Field("time")
    private String time;

    @Field("to")
    private String to;

    private boolean repeat;

    @Field(name="repeatOptions")
    private boolean[] weekdaysRepeat;

    @Field(name="selectedAction")
    private String selectAction;

    private String actionValue;

    @Override
    public String toString() {
        return "Schedule{" +
                "time=" + time +
                ", repeat=" + repeat +
                ", weekdaysRepeat=" + Arrays.toString(weekdaysRepeat) +
                ", selectAction='" + selectAction + '\'' +
                ", actionValue='" + actionValue + '\'' +
                '}';
    }
}
