package com.example.iot_project.entity;

import org.bson.json.JsonObject;
import org.springframework.cglib.core.Local;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Arrays;

public class Schedule{
    @Field("set")
    private String time;
    private boolean repeat;
    @Field(name="repeatOptions")
    private boolean[] weekdaysRepeat;
    @Field(name="selectedAction")
    private String selectAction;
    private String actionValue;

    public String getTime() {
        return time;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public boolean[] getWeekdaysRepeat() {
        return weekdaysRepeat;
    }

    public String getSelectAction() {
        return selectAction;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

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
