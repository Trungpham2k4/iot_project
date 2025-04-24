package com.example.iot_project.service;

import io.github.cdimascio.dotenv.Dotenv;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AdafruitRequestManagerService {
    private final MqttClient mqttClient;

    private static final Dotenv dotenv = Dotenv.load();
    private static final String USER_NAME = dotenv.get("ADA_FRUIT_USER");

    private final ScheduleService scheduleService;

    private final DeviceUpdateService deviceUpdateService;

    @Autowired
    @Lazy
    public AdafruitRequestManagerService(MqttClient client, ScheduleService scheduleService, DeviceUpdateService deviceUpdateService){
        this.mqttClient = client;
        this.scheduleService = scheduleService;
        this.deviceUpdateService = deviceUpdateService;
    }

    public synchronized void publish(String feed, String payload){
        String topic = USER_NAME + "/feeds/" + feed;
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            mqttClient.publish(topic, message);
            System.out.println("Published: " + payload + " to topic: " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }

    }
    @Scheduled(fixedRate = 10000)
    public void scheduleTask() {
        deviceUpdateService.clientListenerAndUpdate();
        scheduleService.checkSchedule();
    }

}
