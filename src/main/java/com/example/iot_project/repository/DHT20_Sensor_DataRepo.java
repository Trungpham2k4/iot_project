package com.example.iot_project.repository;

import com.example.iot_project.model.DHT20_Sensor_Data;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DHT20_Sensor_DataRepo extends MongoRepository<DHT20_Sensor_Data,Long> {
}
