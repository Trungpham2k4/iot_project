package com.example.iot_project.service;

import com.example.iot_project.entity.SensorData;
import com.example.iot_project.repository.SensorDataRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShowService {

    private final SensorDataRepo sensorDataRepo;

    @Autowired
    public ShowService(SensorDataRepo sensorDataRepo){
        this.sensorDataRepo = sensorDataRepo;
    }

    public List<SensorData> getSensorData(){
        return sensorDataRepo.findAll();
    }

}
