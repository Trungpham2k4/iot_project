package com.example.iot_project.controller;


import com.example.iot_project.model.SensorData;
import com.example.iot_project.service.ShowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/index")
public class HomeController {

    @Autowired
    private ShowService showService;

    @GetMapping("/show")
    public ResponseEntity<List<SensorData>> show(){
        return ResponseEntity.ok().body(showService.getSensorData());
    }
}
