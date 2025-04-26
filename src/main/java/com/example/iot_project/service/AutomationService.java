package com.example.iot_project.service;

import com.example.iot_project.model.Automation;
import com.example.iot_project.model.Device;
import com.example.iot_project.model.Log;
import com.example.iot_project.repository.DeviceRepo;
import com.example.iot_project.repository.LogRepo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

@Service
public class AutomationService {

    private final Dotenv dotenv = Dotenv.load();

    private final Map<String, BiPredicate<Double, Double>> operators = new HashMap<>();

    private final String[] PUB_FEEDS = {
            dotenv.get("ADA_FRUIT_LED_COLOR"), dotenv.get("ADA_FRUIT_LED"), dotenv.get("ADA_FRUIT_FAN")
    };
    private final AdafruitRequestManagerService adafruitRequestManagerService;
    private final DeviceRepo deviceRepo;

    private final LogRepo logRepo;

//    private final AtomicBoolean fanLock = new AtomicBoolean(false);
//    private final AtomicBoolean ledLock = new AtomicBoolean(false);
//
//    private final AtomicInteger prevValue = new AtomicInteger(0);
//    private final AtomicReference<String> prevColor = new AtomicReference<>("#000000");

    public AutomationService(AdafruitRequestManagerService adafruitRequestManagerService, DeviceRepo deviceRepo, LogRepo logRepo){
        this.adafruitRequestManagerService = adafruitRequestManagerService;
        this.deviceRepo = deviceRepo;
        this.logRepo = logRepo;
    }

    @PostConstruct
    private void init(){
        operators.put("<",  (a, b) -> a < b);
        operators.put(">",  (a, b) -> a > b);
        operators.put("=", Double::equals);
    }

    public void checkAutomation(Automation fanAutomation, Automation ledAutomation,
                                Double temperature, Double humidity, Integer prevFan, String prevColor,
                                AtomicBoolean fanLock, AtomicBoolean ledLock, AtomicInteger prevValue,
                                AtomicReference<String> setPrevColor) {
            if(fanAutomation != null){
                if (!fanLock.get()){
                    prevValue.set(prevFan);
                    fanLock.set(true);
                }
                generalCheck(fanAutomation, temperature, humidity);
            }else{
                /// luu ca gia tri, status, log cua quat
                if(fanLock.get()){
                    fanLock.set(false);
                    Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElseThrow(() -> new RuntimeException("No device found"));
                    fan.setFanSpeed(prevValue.get());
                    fan.setStatus(prevValue.get() == 0 ? "OFF" : "ON");
                    deviceRepo.save(fan);

                    Log log = new Log();
                    log.setLogId(UUID.randomUUID().toString());
                    log.setFanSpeed(prevValue.get());
                    log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                    log.setLEDColor(null);
                    log.setLEDStatus(null);
                    logRepo.save(log);

                    adafruitRequestManagerService.publish(PUB_FEEDS[2], String.valueOf(prevValue.get()));
                }
            }


            if (ledAutomation != null) {
                if (!ledLock.get()){
                    ledLock.set(true);
                    setPrevColor.set(prevColor);
                }
                generalCheck(ledAutomation, temperature, humidity);
            }else{
                if(ledLock.get()){
                    ledLock.set(false);
                    Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElseThrow(() -> new RuntimeException("No device found"));
                    led.setLedColor(setPrevColor.get());
                    led.setStatus(Objects.equals(setPrevColor.get(), "#000000") ? "OFF" : "ON");

                    Log log = new Log();
                    log.setLogId(UUID.randomUUID().toString());
                    log.setFanSpeed(null);
                    log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                    log.setLEDColor(setPrevColor.get());
                    log.setLEDStatus(Objects.equals(setPrevColor.get(), "#000000") ? "OFF" : "ON");

                    logRepo.save(log);
                    deviceRepo.save(led);
                    adafruitRequestManagerService.publish(PUB_FEEDS[1],Objects.equals(setPrevColor.get(), "#000000") ? "0" : "1" );
                    adafruitRequestManagerService.publish(PUB_FEEDS[0], setPrevColor.get());
                }

            }
    }

    private void generalCheck(Automation automation,
                              Double temperature,
                              Double humidity){
        if (Objects.equals(automation.getData(), "temperature")) {
            checkCondition(automation.getTask(), automation.getDeviceValue(), automation.getDevice(),
                    automation.getCondition(), Double.parseDouble(automation.getValue()), temperature);

        }
        if (Objects.equals(automation.getData(), "humidity")) {
            checkCondition(automation.getTask(), automation.getDeviceValue(), automation.getDevice(),
                    automation.getCondition(), Double.parseDouble(automation.getValue()), humidity);
        }

    }

    public void checkCondition(String action, String actionValue, String device,
                                String condition,  double conditionValue, double sensorValue) {
        switch (device) {
            case "fan" -> {
                if (operators.get(condition).test(sensorValue, conditionValue)) {
                    if (Objects.equals(action, "turn_on")) {
                        adafruitRequestManagerService.publish(PUB_FEEDS[2], "50");
                        int speed = Integer.parseInt("50");

                        Log log = new Log();
                        log.setLogId(UUID.randomUUID().toString());
                        log.setLEDColor(null);
                        log.setFanSpeed(speed);
                        log.setLEDStatus(null);
                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                        logRepo.save(log);

                        Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                        assert fan != null;
                        fan.setFanSpeed(speed);
                        fan.setStatus("ON");
                        deviceRepo.save(fan);
                    }
                    if (Objects.equals(action, "turn_off")){
                        adafruitRequestManagerService.publish(PUB_FEEDS[2], "0");
                        Log log = new Log();
                        log.setLogId(UUID.randomUUID().toString());
                        log.setLEDColor(null);
                        log.setFanSpeed(Integer.valueOf("0"));
                        log.setLEDStatus(null);
                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                        logRepo.save(log);

                        Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                        assert fan != null;
                        fan.setFanSpeed(Integer.valueOf("0"));
                        fan.setStatus("OFF");
                        deviceRepo.save(fan);
                    }
                    if (Objects.equals(action, "set_value")){
                        adafruitRequestManagerService.publish(PUB_FEEDS[2], actionValue);

                        Log log = new Log();
                        log.setLogId(UUID.randomUUID().toString());
                        log.setLEDColor(null);
                        log.setFanSpeed(Integer.parseInt(actionValue));
                        log.setLEDStatus(null);
                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                        logRepo.save(log);

                        Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                        assert fan != null;
                        fan.setFanSpeed(Integer.parseInt(actionValue));
                        fan.setStatus(Integer.parseInt(actionValue) == 0 ? "OFF" : "ON");
                        deviceRepo.save(fan);
                    }
                }

            }
            case "led" -> {
                if (operators.get(condition).test(sensorValue, conditionValue)) {
                    if (Objects.equals(action, "turn_on")) {
                        String color = "#ff0000";
                        adafruitRequestManagerService.publish(PUB_FEEDS[1], "1");
                        adafruitRequestManagerService.publish(PUB_FEEDS[0], color);

                        Log log = new Log();
                        log.setLogId(UUID.randomUUID().toString());
                        log.setLEDColor(color);
                        log.setFanSpeed(null);
                        log.setLEDStatus("ON");
                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                        logRepo.save(log);

                        Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);
                        assert led != null;
                        led.setLedColor(color);
                        led.setStatus("ON");
                        deviceRepo.save(led);
                    }
                    if (Objects.equals(action, "turn_off")) {
                        String color = "#000000";
                        adafruitRequestManagerService.publish(PUB_FEEDS[1], "0");
                        adafruitRequestManagerService.publish(PUB_FEEDS[0], color);

                        Log log = new Log();
                        log.setLogId(UUID.randomUUID().toString());
                        log.setLEDColor(color);
                        log.setFanSpeed(null);
                        log.setLEDStatus("OFF");
                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                        logRepo.save(log);

                        Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);
                        assert led != null;
                        led.setLedColor(color);
                        led.setStatus("OFF");
                        deviceRepo.save(led);
                    }

                    if (Objects.equals(action, "set_value")) {
                        adafruitRequestManagerService.publish(PUB_FEEDS[1], Objects.equals(actionValue, "#000000") ? "0" : "1");
                        adafruitRequestManagerService.publish(PUB_FEEDS[0], actionValue);

                        Log log = new Log();
                        log.setLogId(UUID.randomUUID().toString());
                        log.setLEDColor(actionValue);
                        log.setFanSpeed(null);
                        log.setLEDStatus(Objects.equals(actionValue, "#000000") ? "OFF" : "ON");
                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                        logRepo.save(log);

                        Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);
                        assert led != null;
                        led.setLedColor(actionValue);
                        led.setStatus(Objects.equals(actionValue, "#000000") ? "OFF" : "ON");
                        deviceRepo.save(led);
                    }
                }
            }
        }
    }
}
