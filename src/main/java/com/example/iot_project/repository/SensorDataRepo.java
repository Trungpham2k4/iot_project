package com.example.iot_project.repository;

import com.example.iot_project.model.SensorData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SensorDataRepo extends MongoRepository<SensorData, Long> {

}
