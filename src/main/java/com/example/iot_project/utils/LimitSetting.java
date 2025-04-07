package com.example.iot_project.utils;

import org.springframework.stereotype.Component;

@Component
public class LimitSetting {
    private static double HUMIDITY_LIMIT = 0.0;
    private static double TEMPERATURE_LIMIT = 0.0;
    private static double LIGHT_LIMIT = 0.0;
    private static double FAN_LIMIT = 0.0;

    public static double getHumidityLimit() {
        return HUMIDITY_LIMIT;
    }

    public static void setHumidityLimit(double humidityLimit) {
        HUMIDITY_LIMIT = humidityLimit;
    }

    public static double getTemperatureLimit() {
        return TEMPERATURE_LIMIT;
    }

    public static void setTemperatureLimit(double temperatureLimit) {
        TEMPERATURE_LIMIT = temperatureLimit;
    }

    public static double getLightLimit() {
        return LIGHT_LIMIT;
    }

    public static void setLightLimit(double lightLimit) {
        LIGHT_LIMIT = lightLimit;
    }

    public static double getFanLimit() {
        return FAN_LIMIT;
    }

    public static void setFanLimit(double fanLimit) {
        FAN_LIMIT = fanLimit;
    }
}
