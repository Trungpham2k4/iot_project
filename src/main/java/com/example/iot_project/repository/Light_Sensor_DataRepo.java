package com.example.iot_project.repository;


import com.example.iot_project.entity.Light_Sensor_Data;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Light_Sensor_DataRepo extends MongoRepository<Light_Sensor_Data,String> {
}
