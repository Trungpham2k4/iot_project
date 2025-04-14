package com.example.iot_project.service;

import com.example.iot_project.model.Device;
import com.example.iot_project.model.Log;
import com.example.iot_project.repository.DeviceRepo;
import com.example.iot_project.repository.LogRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class DeviceUpdateService {

    private final AdafruitRequestManagerService adafruitRequestManagerService;
    private final DeviceRepo deviceRepo;

    private final LogRepo logRepo;

    @Autowired
    public DeviceUpdateService(AdafruitRequestManagerService adafruitRequestManagerService, DeviceRepo deviceRepo, LogRepo logRepo){
        this.adafruitRequestManagerService = adafruitRequestManagerService;
        this.deviceRepo = deviceRepo;
        this.logRepo = logRepo;
    }

    public void clientListenerAndUpdate(){
        System.out.println("Retrieve from client");
        List<Device> devices = deviceRepo.findAll();
        for(Device device : devices){
            LocalDateTime date = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            if (device.getFanSpeed() != null && device.getEditFlag()){
                adafruitRequestManagerService.publish("fan",Integer.toString(device.getFanSpeed()));
                device.setStatus(device.getFanSpeed() == 0 ? "OFF" : "ON");

                Log log = new Log();
                log.setLogId(UUID.randomUUID().toString());
                log.setLEDColor(null);
                log.setFanSpeed(device.getFanSpeed());
                log.setLEDStatus(null);
                log.setTimestamp(date);
                logRepo.save(log);
            }else if (device.getLedColor() != null && device.getEditFlag()){

                adafruitRequestManagerService.publish("led", Objects.equals(device.getStatus(), "ON") ? "1" : "0");
                String color;
                if (Objects.equals(device.getStatus(), "ON")){
                    if (Objects.equals(device.getLedColor(), "#000000")){
                        color = "#ff0000";
                    }else{
                        color = device.getLedColor();
                    }
                }else{
                    color = "#000000";
                }
                adafruitRequestManagerService.publish("led-color", color);
                device.setStatus(Objects.equals(color, "#000000") ? "OFF" : "ON");
                Log log = new Log();
                log.setLogId(UUID.randomUUID().toString());
                log.setLEDColor(color);
                log.setFanSpeed(null);
                log.setLEDStatus(device.getStatus());
                log.setTimestamp(date);
                logRepo.save(log);

            }
            device.setEditFlag(false);
        }
        deviceRepo.saveAll(devices);
    }
}
