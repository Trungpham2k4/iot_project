package com.example.iot_project.repository;


import com.example.iot_project.entity.Log;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogRepo extends MongoRepository<Log,String> {
}
