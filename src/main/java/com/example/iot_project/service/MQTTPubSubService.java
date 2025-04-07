package com.example.iot_project.service;

import com.example.iot_project.entity.*;
import com.example.iot_project.repository.*;
import com.example.iot_project.utils.LimitSetting;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.*;

@Service
public class MQTTPubSubService {
//    private static final String USER_NAME = "TrungPham";
//    private static final String SERVER_URI = "tcp://io.adafruit.com:1883";
//    private static final String[] FEEDS_NUMBER = {"smart-room-humid", "smart-room-light", "smart-room-temp"}; // feed lưu dữ liệu số
    private static final Dotenv dotenv = Dotenv.load();
    private static final String USER_NAME = dotenv.get("ADA_FRUIT_USER");
    private static final String KEY = dotenv.get("ADA_FRUIT_KEY");
    private static final String SERVER_URI = dotenv.get("ADA_FRUIT_SERVER_URI");
    private static final String[] FEEDS = {dotenv.get("ADA_FRUIT_LED_COLOR"), dotenv.get("ADA_FRUIT_FAN"),
            dotenv.get("ADA_FRUIT_HUMID"), dotenv.get("ADA_FRUIT_LIGHT"), dotenv.get("ADA_FRUIT_TEMP"), };

    private static final String[] DEVICE_NAME = {"FAN_1", "LED_1"};

    private static final ConcurrentMap<String,Double> sessionData = new ConcurrentHashMap<>();

//    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private MqttClient mqttClient;  // Biến toàn cục để giữ kết nối

    private final LogRepo logRepo;
    private final DHT20_Sensor_DataRepo dht20_sensor_dataRepo;
    private final Light_Sensor_DataRepo light_sensor_dataRepo;
    private final DeviceRepo deviceRepo;

    private final LimitSetting limitSetting;

    @Autowired
    public MQTTPubSubService(LogRepo logRepo, DHT20_Sensor_DataRepo DHT20repo, Light_Sensor_DataRepo light_sensor_dataRepo, DeviceRepo deviceRepo, LimitSetting limitSetting ) {
        this.logRepo = logRepo;
        this.dht20_sensor_dataRepo = DHT20repo;
        this.light_sensor_dataRepo = light_sensor_dataRepo;
        this.deviceRepo = deviceRepo;
        this.limitSetting = limitSetting;
    }

    @PostConstruct
    private void init(){
        long numRecords = deviceRepo.count();
        if (numRecords == 0){
            List<Device> lst = new ArrayList<>();
            for (String name : DEVICE_NAME){
                Device device = new Device();
                device.setDeviceId(name);
                device.setStatus("OFF");
                lst.add(device);

            }
            deviceRepo.saveAll(lst);
        }
        connectMQTT();
    }

    private void connectMQTT() {
        try{
            mqttClient = new MqttClient(SERVER_URI, MqttClient.generateClientId());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(USER_NAME);
            options.setPassword(KEY.toCharArray());
            options.setAutomaticReconnect(true); // Tự động kết nối lại nếu mất
            options.setCleanSession(false); // Giữ session MQTT

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Lost connection. Reconnecting...");
//                    try {
//                        Thread.sleep(5000); // Chờ 5 giây trước khi kết nối lại
//                        connectMQTT();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage mqttMessage) {
                    String message = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
                    System.out.println("Received: " + message);
                    System.out.println(topic);

                    String[] parts = topic.split("/");
                    String feed_id = parts[2];

                    LocalDateTime date = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

                    switch (feed_id){
                        case "humidity", "temperature" -> {
                            synchronized (sessionData){
                                if (Objects.equals("humidity", feed_id)) {
                                    sessionData.put("humidity", Double.parseDouble(message));

                                    if(sessionData.containsKey("temperature")){
                                        saveData(date);
                                    }

                                } else {
                                    sessionData.put("temperature", Double.parseDouble(message));

                                    if(sessionData.containsKey("humidity")){
                                        saveData(date);
                                    }

                                }
                            }
                        }
                        case "light" -> {
                            Light_Sensor_Data data = new Light_Sensor_Data();
                            data.setDataId(UUID.randomUUID().toString());
                            data.setIntensity(Double.parseDouble(message));
                            data.setTimestamp(date);
                            light_sensor_dataRepo.save(data);
                        }
//                        case "fan" ->{
//                            Log log = new Log();
//                            log.setLogId(UUID.randomUUID().toString());
//                            log.setLEDColor(null);
//                            log.setFanSpeed(Double.parseDouble(message));
//                            log.setLEDStatus(null);
//                            log.setTimestamp(date);
//
//                            Device fan = new Device();
//                            fan.setDeviceId("FAN_1");
//                            fan.setFanSpeed(Double.parseDouble(message));
//                            fan.setLedColor(null);
//
//                            if (Double.parseDouble(message) > 0){
//                                fan.setStatus("ON");
//                            }else {
//                                fan.setStatus("OFF");
//                            }
//                            fan.setEditFlag("false");
//                            deviceRepo.save(fan);
//
//                            try {
//                                logRepo.save(log);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//
//
//                        }
//                        case "led-color" ->{
//                            Log log = new Log();
//                            log.setLogId(UUID.randomUUID().toString());
//                            log.setLEDColor(message);
//                            log.setFanSpeed(null);
//                            log.setLEDStatus(Objects.equals(message, "#000000") ? "OFF"  : "ON");
//                            log.setTimestamp(date);
//
//                            Device led = new Device();
//                            led.setDeviceId("LED_1");
//                            led.setLedColor(message);
//                            led.setFanSpeed(null);
//
//                            led.setStatus(Objects.equals(message, "#000000") ? "OFF" : "ON");
//                            led.setEditFlag("false");
//                            deviceRepo.save(led);
//
//                            logRepo.save(log);
//                        }
                    }


                    System.out.println("Save successfully");
                }
                private void saveData(LocalDateTime date){
                    DHT20_Sensor_Data data = new DHT20_Sensor_Data();
                    data.setDataId(UUID.randomUUID().toString());
                    data.setTimestamp(date);
                    data.setHumidity(sessionData.get("humidity"));
                    data.setTemperature(sessionData.get("temperature"));
                    dht20_sensor_dataRepo.save(data);

                    sessionData.clear();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }

            });

            mqttClient.connect(options);
            for (String feed : FEEDS) {
                mqttClient.subscribe(USER_NAME + "/feeds/" + feed);
            }
            System.out.println("MQTT Connected & Subscribed!");


        }catch(Exception e){
            e.printStackTrace();
        }

    }

    public void publish(String feed, String payload) {
        String topic = USER_NAME + "/feeds/" + feed;
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            mqttClient.publish(topic, message);
            System.out.println("Published: " + payload + " to topic: " + topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 10000)
    private void clientListenerAndUpdate(){
        System.out.println("Retrieve from client");
        List<Device> devices = deviceRepo.findAll();
        for(Device device : devices){
            LocalDateTime date = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            if (device.getFanSpeed() != null && device.getEditFlag()){
                publish("fan",Double.toString(device.getFanSpeed()));


                Log log = new Log();
                log.setLogId(UUID.randomUUID().toString());
                log.setLEDColor(null);
                log.setFanSpeed(device.getFanSpeed());
                log.setLEDStatus(null);
                log.setTimestamp(date);
                logRepo.save(log);
            }else if (device.getLedColor() != null && device.getEditFlag()){

                publish("led", Objects.equals(device.getStatus(), "ON") ? "1" : "0");
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
                publish("led-color", color);

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
