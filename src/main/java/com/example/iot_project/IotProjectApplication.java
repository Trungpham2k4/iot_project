package com.example.iot_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.ZoneId;

@EnableScheduling
@SpringBootApplication
public class IotProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(IotProjectApplication.class, args);
//		System.out.println("Danh sách múi giờ hợp lệ:");
//		ZoneId.getAvailableZoneIds().forEach(System.out::println);
//		System.out.println(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
//		System.out.println(LocalDateTime.now(ZoneId.systemDefault()));
	}

}
