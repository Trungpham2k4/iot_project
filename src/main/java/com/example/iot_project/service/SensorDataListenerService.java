package com.example.iot_project.service;

import com.example.iot_project.model.*;
import com.example.iot_project.repository.DHT20_Sensor_DataRepo;
import com.example.iot_project.repository.DeviceRepo;
import com.example.iot_project.repository.Light_Sensor_DataRepo;
import com.example.iot_project.repository.LogRepo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class SensorDataListenerService {
//    private static final String USER_NAME = "TrungPham";
//    private static final String SERVER_URI = "tcp://io.adafruit.com:1883";
//    private static final String[] FEEDS_NUMBER = {"smart-room-humid", "smart-room-light", "smart-room-temp"}; // feed lưu dữ liệu số
    private static final Dotenv dotenv = Dotenv.load();
    private static final String USER_NAME = dotenv.get("ADA_FRUIT_USER");
    private static final String[] SUB_FEEDS = {
            dotenv.get("ADA_FRUIT_HUMID"), dotenv.get("ADA_FRUIT_LIGHT"), dotenv.get("ADA_FRUIT_TEMP")
    };
    private final String[] PUB_FEEDS = {
            dotenv.get("ADA_FRUIT_LED_COLOR"), dotenv.get("ADA_FRUIT_LED"), dotenv.get("ADA_FRUIT_FAN")
    };

    private static final ConcurrentMap<String,Double> sessionData = new ConcurrentHashMap<>();

    private final MqttClient mqttClient;  // Biến toàn cục để giữ kết nối
    private final DHT20_Sensor_DataRepo dht20_sensor_dataRepo;
    private final Light_Sensor_DataRepo light_sensor_dataRepo;
    private final DeviceRepo deviceRepo;

    private final AutomationService automationService;

    private final AtomicBoolean fanLock = new AtomicBoolean(false);
    private final AtomicBoolean ledLock = new AtomicBoolean(false);

    private final AtomicInteger prevValue = new AtomicInteger(0);
    private final AtomicReference<String> prevColor = new AtomicReference<>("#000000");
    private final AdafruitRequestManagerService adafruitRequestManagerService;
    private final LogRepo logRepo;

    @Autowired
    public SensorDataListenerService(DHT20_Sensor_DataRepo DHT20repo, Light_Sensor_DataRepo light_sensor_dataRepo,
                                     DeviceRepo deviceRepo, MqttClient mqttClient,
                                     AutomationService automationService, AdafruitRequestManagerService adafruitRequestManagerService, LogRepo logRepo) {
        this.dht20_sensor_dataRepo = DHT20repo;
        this.light_sensor_dataRepo = light_sensor_dataRepo;
        this.deviceRepo = deviceRepo;
        this.mqttClient = mqttClient;
        this.automationService = automationService;
        this.adafruitRequestManagerService = adafruitRequestManagerService;
        this.logRepo = logRepo;
    }

    @PostConstruct
    private void init(){
//        long numRecords = deviceRepo.count();
//        if (numRecords == 0){
//            List<Device> lst = new ArrayList<>();
//            for (String name : DEVICE_NAME){
//                Device device = new Device();
//                device.setDeviceId(name);
//                device.setStatus("OFF");
//                lst.add(device);
//
//            }
//            deviceRepo.saveAll(lst);
//        }
        connectMQTT();

    }



    private void connectMQTT() {
        try{
//            mqttClient = new MqttClient(SERVER_URI, MqttClient.generateClientId());
//            MqttConnectOptions options = new MqttConnectOptions();
//            options.setUserName(USER_NAME);
//            options.setPassword(KEY.toCharArray());
//            options.setAutomaticReconnect(true); // Tự động kết nối lại nếu mất
//            options.setCleanSession(false); // Giữ session MQTT

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
//                    System.out.println("Lost connection. Reconnecting...");
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


                    String[] parts = topic.split("/");
                    String feed_id = parts[2];

                    LocalDateTime date = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

                    Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                    Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);
                    List<Automation> fanAutomation = fan.getAutomation();
                    List<Automation> ledAutomation = led.getAutomation();


                    switch (feed_id){
                        case "humidity", "temperature" -> {
                            System.out.println("Received: " + message);
                            System.out.println(topic);
                            synchronized (sessionData){
                                if (Objects.equals("humidity", feed_id)) {
                                    sessionData.put("humidity", Double.parseDouble(message));

                                    if(sessionData.containsKey("temperature")){
                                        automationService.checkAutomation(fanAutomation, ledAutomation,
                                                sessionData.get("temperature"), sessionData.get("humidity"), fan.getFanSpeed(),
                                                led.getLedColor(), fanLock, ledLock, prevValue, prevColor);
                                        saveData(date);
                                    }

                                } else {
                                    sessionData.put("temperature", Double.parseDouble(message));

                                    if(sessionData.containsKey("humidity")){
                                        automationService.checkAutomation(fanAutomation, ledAutomation,
                                                sessionData.get("temperature"), sessionData.get("humidity"), fan.getFanSpeed(),
                                                led.getLedColor(), fanLock, ledLock, prevValue, prevColor);
                                        saveData(date);
                                    }

                                }
                            }
                        }
                        case "light" -> {
                            System.out.println("Received: " + message);
                            System.out.println(topic);
                            Light_Sensor_Data data = new Light_Sensor_Data();
                            data.setDataId(UUID.randomUUID().toString());
                            data.setIntensity(Double.parseDouble(message));
                            data.setTimestamp(date);
                            light_sensor_dataRepo.save(data);

                            if (!fanAutomation.isEmpty()){
                                if (!fanLock.get()){
                                    fanLock.set(true);
                                    prevValue.set(fan.getFanSpeed());
                                }
                                if (Objects.equals(fanAutomation.getFirst().getData(),"light")){
                                    automationService.checkCondition(fanAutomation.getFirst().getTask(), fanAutomation.getFirst().getDeviceValue(),
                                            fanAutomation.getFirst().getDevice(), fanAutomation.getFirst().getCondition(),
                                            Double.parseDouble(fanAutomation.getFirst().getValue()), data.getIntensity(),
                                            fanLock, ledLock, prevValue, prevColor);
                                }
                            }else{
                                if (fanLock.get()){
                                    fanLock.set(false);
                                    fan.setFanSpeed(prevValue.get());
                                    fan.setStatus(prevValue.get() == 0 ? "OFF" : "ON");

                                    Log log = new Log();
                                    log.setLogId(UUID.randomUUID().toString());
                                    log.setFanSpeed(prevValue.get());
                                    log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                                    log.setLEDColor(null);
                                    log.setLEDStatus(null);

                                    logRepo.save(log);
                                    deviceRepo.save(fan);
                                    adafruitRequestManagerService.publish(PUB_FEEDS[2], String.valueOf(prevValue.get()));
                                }
                            }

                            ///  Demo lỗi do cô k xóa automation, k xóa => K release lock, k set được cái prevColor là màu hiện tại
                            /// => Luôn dùng prev cũ (màu đỏ)
                            if (!ledAutomation.isEmpty()){
                                if (!ledLock.get()){
                                    ledLock.set(true);
                                    prevColor.set(led.getLedColor());
                                }
                                if (Objects.equals(ledAutomation.getFirst().getData(),"light")){
                                    automationService.checkCondition(ledAutomation.getFirst().getTask(), ledAutomation.getFirst().getDeviceValue(),
                                            ledAutomation.getFirst().getDevice(), ledAutomation.getFirst().getCondition(),
                                            Double.parseDouble(ledAutomation.getFirst().getValue()), data.getIntensity(),
                                            fanLock, ledLock, prevValue, prevColor);
                                }
                            }else{
                                if (ledLock.get()){
                                    ledLock.set(false);
                                    led.setLedColor(prevColor.get());
                                    led.setStatus(Objects.equals(prevColor.get(), "#000000") ? "OFF" : "ON");

                                    Log log = new Log();
                                    log.setLogId(UUID.randomUUID().toString());
                                    log.setFanSpeed(null);
                                    log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                                    log.setLEDColor(prevColor.get());
                                    log.setLEDStatus(Objects.equals(prevColor.get(), "#000000") ? "OFF" : "ON");

                                    logRepo.save(log);
                                    deviceRepo.save(led);
                                    adafruitRequestManagerService.publish(PUB_FEEDS[1],Objects.equals(prevColor.get(), "#000000") ? "0" : "1" );
                                    adafruitRequestManagerService.publish(PUB_FEEDS[0], prevColor.get());
                                }
                            }
                        }
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

//            mqttClient.connect(options);
            for (String feed : SUB_FEEDS) {
                mqttClient.subscribe(USER_NAME + "/feeds/" + feed);
            }
            System.out.println("MQTT Connected & Subscribed!");


        }catch(Exception e){
            log.error(e.getMessage());
        }

    }
}
