package com.example.iot_project.model;

import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Arrays;

public class Schedule{
    @Field("set")
    private String set;

    @Field("time")
    private String time;
    private boolean repeat;
    @Field(name="repeatOptions")
    private boolean[] weekdaysRepeat;
    @Field(name="selectedAction")
    private String selectAction;
    private String actionValue;

    public String getTime() {
        return set;
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
                "time=" + set +
                ", repeat=" + repeat +
                ", weekdaysRepeat=" + Arrays.toString(weekdaysRepeat) +
                ", selectAction='" + selectAction + '\'' +
                ", actionValue='" + actionValue + '\'' +
                '}';
    }
}
