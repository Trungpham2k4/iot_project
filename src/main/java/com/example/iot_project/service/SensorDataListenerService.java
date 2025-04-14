package com.example.iot_project.service;

import com.example.iot_project.model.Automation;
import com.example.iot_project.model.DHT20_Sensor_Data;
import com.example.iot_project.model.Device;
import com.example.iot_project.model.Light_Sensor_Data;
import com.example.iot_project.repository.DHT20_Sensor_DataRepo;
import com.example.iot_project.repository.DeviceRepo;
import com.example.iot_project.repository.Light_Sensor_DataRepo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class SensorDataListenerService {
//    private static final String USER_NAME = "TrungPham";
//    private static final String SERVER_URI = "tcp://io.adafruit.com:1883";
//    private static final String[] FEEDS_NUMBER = {"smart-room-humid", "smart-room-light", "smart-room-temp"}; // feed lưu dữ liệu số
    private static final Dotenv dotenv = Dotenv.load();
    private static final String USER_NAME = dotenv.get("ADA_FRUIT_USER");
    private static final String[] SUB_FEEDS = {
            dotenv.get("ADA_FRUIT_HUMID"), dotenv.get("ADA_FRUIT_LIGHT"), dotenv.get("ADA_FRUIT_TEMP")
    };

    private static final ConcurrentMap<String,Double> sessionData = new ConcurrentHashMap<>();

    private final MqttClient mqttClient;  // Biến toàn cục để giữ kết nối
    private final DHT20_Sensor_DataRepo dht20_sensor_dataRepo;
    private final Light_Sensor_DataRepo light_sensor_dataRepo;
    private final DeviceRepo deviceRepo;

    private final AutomationService automationService;


    @Autowired
    public SensorDataListenerService(DHT20_Sensor_DataRepo DHT20repo, Light_Sensor_DataRepo light_sensor_dataRepo,
                                     DeviceRepo deviceRepo, MqttClient mqttClient,
                                     AutomationService automationService) {
        this.dht20_sensor_dataRepo = DHT20repo;
        this.light_sensor_dataRepo = light_sensor_dataRepo;
        this.deviceRepo = deviceRepo;
        this.mqttClient = mqttClient;
        this.automationService = automationService;
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

                    assert fan != null;
                    assert led != null;
                    Automation fanAutomation = fan.getAutomation();
                    Automation ledAutomation = led.getAutomation();

                    String fanCondition = null;
                    double fanValue = 0.0;
                    String fanAction = null;
                    String fanDeviceValue = null;
                    String fanDevice = null;

                    String ledCondition = null;
                    double ledValue = 0.0;
                    String ledAction = null;
                    String ledDeviceValue = null;
                    String ledDevice = null;

                    if (fanAutomation != null){
                        fanCondition = fanAutomation.getCondition();
                        fanValue = Double.parseDouble(fanAutomation.getValue());
                        fanAction = fanAutomation.getTask();
                        fanDeviceValue = fanAutomation.getDeviceValue();
                        fanDevice = fanAutomation.getDevice();
                    }
                    if (ledAutomation != null){
                        ledCondition = ledAutomation.getCondition();
                        ledValue = Double.parseDouble(ledAutomation.getValue());
                        ledAction = ledAutomation.getTask();
                        ledDeviceValue = ledAutomation.getDeviceValue();
                        ledDevice = ledAutomation.getDevice();
                    }


                    switch (feed_id){
                        case "humidity", "temperature" -> {
                            System.out.println("Received: " + message);
                            System.out.println(topic);
                            synchronized (sessionData){
                                if (Objects.equals("humidity", feed_id)) {
                                    sessionData.put("humidity", Double.parseDouble(message));

                                    if(sessionData.containsKey("temperature")){
                                        automationService.checkAutomation(fanAutomation, ledAutomation, fanCondition,
                                                fanValue, fanAction, fanDeviceValue, fanDevice, ledCondition, ledValue,
                                                ledAction, ledDeviceValue, ledDevice,
                                                sessionData.get("temperature"), sessionData.get("humidity"));
                                        saveData(date);
                                    }

                                } else {
                                    sessionData.put("temperature", Double.parseDouble(message));

                                    if(sessionData.containsKey("humidity")){
                                        automationService.checkAutomation(fanAutomation, ledAutomation, fanCondition,
                                                fanValue, fanAction, fanDeviceValue, fanDevice, ledCondition, ledValue,
                                                ledAction, ledDeviceValue, ledDevice,
                                                sessionData.get("temperature"), sessionData.get("humidity"));
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

                            if (fanAutomation != null){
                                if (Objects.equals(fanAutomation.getData(),"light")){
                                    automationService.checkCondition(fanAction, fanDeviceValue, fanDevice, fanCondition,
                                            fanValue, data.getIntensity());
                                }
                            }
                            if (ledAutomation != null){
                                if (Objects.equals(ledAutomation.getData(),"light")){
                                    automationService.checkCondition(ledAction, ledDeviceValue, ledDevice, ledCondition,
                                            ledValue, data.getIntensity());
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
            e.printStackTrace();
        }

    }
}
