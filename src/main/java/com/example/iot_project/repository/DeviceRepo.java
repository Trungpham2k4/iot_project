package com.example.iot_project.repository;

import com.example.iot_project.model.Device;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepo extends MongoRepository<Device,String> {

    Optional<Device> getDeviceByDeviceId(String deviceId);
}
