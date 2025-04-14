package com.example.iot_project.service;

import com.example.iot_project.model.Automation;
import com.example.iot_project.model.Device;
import com.example.iot_project.model.Log;
import com.example.iot_project.repository.DeviceRepo;
import com.example.iot_project.repository.LogRepo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
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
                                 String fanCondition, double fanValue, String fanAction, String fanDeviceValue,
                                 String fanDevice, String ledCondition, double ledValue, String ledAction,
                                 String ledDeviceValue, String ledDevice, Double temperature, Double humidity) {
        if (fanAutomation != null) {
            if (Objects.equals(fanAutomation.getData(), "temperature")) {
                checkCondition(fanAction, fanDeviceValue, fanDevice, fanCondition,
                        fanValue, temperature);

            }
            if (Objects.equals(fanAutomation.getData(), "humidity")) {
                checkCondition(fanAction, fanDeviceValue, fanDevice, fanCondition,
                        fanValue, humidity);
            }
        }
        if (ledAutomation != null){
            if (Objects.equals(ledAutomation.getData(), "temperature")){
                checkCondition(ledAction, ledDeviceValue, ledDevice, ledCondition,
                        ledValue, temperature);
            }
            if (Objects.equals(ledAutomation.getData(), "humidity")){
                checkCondition(ledAction, ledDeviceValue, ledDevice, ledCondition,
                        ledValue, humidity);
            }
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
                        adafruitRequestManagerService.publish(PUB_FEEDS[1], Objects.equals(actionValue, "#000000") ? "OFF" : "ON");
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
