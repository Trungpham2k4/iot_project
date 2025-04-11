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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiPredicate;

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
            dotenv.get("ADA_FRUIT_HUMID"), dotenv.get("ADA_FRUIT_LIGHT"), dotenv.get("ADA_FRUIT_TEMP"), dotenv.get("ADA_FRUIT_LED")};

    private static final String[] DEVICE_NAME = {"FAN_1", "LED_1"};

    private static final ConcurrentMap<String,Double> sessionData = new ConcurrentHashMap<>();

//    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private MqttClient mqttClient;  // Biến toàn cục để giữ kết nối

    private final LogRepo logRepo;
    private final DHT20_Sensor_DataRepo dht20_sensor_dataRepo;
    private final Light_Sensor_DataRepo light_sensor_dataRepo;
    private final DeviceRepo deviceRepo;

    private final LimitSetting limitSetting;

    private static int FansRunToday = 0;
    private static int LedsRunToday = 0;

    Map<String, BiPredicate<Double, Double>> operators = new HashMap<>();
    Map<Integer, Integer> Fanruns = new HashMap<>();
    Map<Integer, Integer> Ledruns = new HashMap<>();


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
        operators.put("<",  (a, b) -> a < b);
        operators.put("<=", (a, b) -> a <= b);
        operators.put(">",  (a, b) -> a > b);
        operators.put(">=", (a, b) -> a >= b);
        operators.put("=", Double::equals); // nếu dùng Integer
        operators.put("!=", (a,b) -> !a.equals(b));


        Fanruns.put(0,0);
        Fanruns.put(1,0);
        Fanruns.put(2,0);
        Fanruns.put(3,0);
        Fanruns.put(4,0);
        Fanruns.put(5,0);
        Fanruns.put(6,0);

        Ledruns.put(0,0);
        Ledruns.put(1,0);
        Ledruns.put(2,0);
        Ledruns.put(3,0);
        Ledruns.put(4,0);
        Ledruns.put(5,0);
        Ledruns.put(6,0);
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

                    Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                    Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);

                    assert fan != null;
                    assert led != null;
                    Automation fanAutomation = fan.getAutomation();
                    Automation ledAutomation = led.getAutomation();

                    String fanData = fanAutomation.getData();
                    String fanCondition = fanAutomation.getCondition();
                    Double fanValue = Double.parseDouble(fanAutomation.getValue());
                    String fanAction = fanAutomation.getTask();
                    String fanDeviceValue = fanAutomation.getDeviceValue();

                    String ledData = ledAutomation.getData();
                    String ledCondition = ledAutomation.getCondition();
                    Double ledValue = Double.parseDouble(ledAutomation.getValue());
                    String ledAction = ledAutomation.getTask();
                    String ledDeviceValue = ledAutomation.getDeviceValue();

                    switch (feed_id){
                        case "humidity", "temperature" -> {
                            synchronized (sessionData){
                                if (Objects.equals("humidity", feed_id)) {
                                    sessionData.put("humidity", Double.parseDouble(message));

                                    if(sessionData.containsKey("temperature")){
                                        if (Objects.equals(fanData, "temperature")){
                                            checkCondition(fanAction, fanDeviceValue, "fan", fanCondition,
                                                    fanValue, sessionData.get("temperature"));

                                        }
                                        if (Objects.equals(fanData, "humidity")){
                                            checkCondition(fanAction, fanDeviceValue, "fan", fanCondition,
                                                    fanValue, sessionData.get("humidity"));
                                        }
                                        if (Objects.equals(ledData, "temperature")){
                                            checkCondition(ledAction, ledDeviceValue, "led", ledCondition,
                                                    ledValue, sessionData.get("temperature"));
                                        }
                                        if (Objects.equals(ledData, "humidity")){
                                            checkCondition(ledAction, ledDeviceValue, "led", ledCondition,
                                                    ledValue, sessionData.get("humidity"));
                                        }
                                        saveData(date);
                                    }

                                } else {
                                    sessionData.put("temperature", Double.parseDouble(message));

                                    if(sessionData.containsKey("humidity")){
                                        if (Objects.equals(fanData, "temperature")){
                                            checkCondition(fanAction, fanDeviceValue, "fan", fanCondition,
                                                    fanValue, sessionData.get("temperature"));

                                        }
                                        if (Objects.equals(fanData, "humidity")){
                                            checkCondition(fanAction, fanDeviceValue, "fan", fanCondition,
                                                    fanValue, sessionData.get("humidity"));
                                        }
                                        if (Objects.equals(ledData, "temperature")){
                                            checkCondition(ledAction, ledDeviceValue, "led", ledCondition,
                                                    ledValue, sessionData.get("temperature"));
                                        }
                                        if (Objects.equals(ledData, "humidity")){
                                            checkCondition(ledAction, ledDeviceValue, "led", ledCondition,
                                                    ledValue, sessionData.get("humidity"));
                                        }
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
                            checkCondition(fanAction, fanDeviceValue, "fan", fanCondition,
                                    fanValue, data.getIntensity());
                            checkCondition(ledAction, ledDeviceValue, "led", ledCondition,
                                    ledValue, data.getIntensity());
                        }
                    }


                    System.out.println("Save successfully");
                }
                private void checkCondition(String action, String actionValue, String device,
                                            String condition,  double conditionValue, double sensorValue) {
                        switch (device) {
                            case "fan" -> {
                                if (operators.get(condition).test(sensorValue, conditionValue)) {
                                    if (Objects.equals(action, "turn_on")) {
                                        publish(FEEDS[1], "50");
                                        double speed = Double.parseDouble("50");

                                        Log log = new Log();
                                        log.setLogId(UUID.randomUUID().toString());
                                        log.setLEDColor(null);
                                        log.setFanSpeed(speed);
                                        log.setLEDStatus(null);
                                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                                        logRepo.save(log);

                                        Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                                        assert fan != null;
                                        fan.setFanSpeed(speed);
                                        fan.setStatus("ON");
                                        deviceRepo.save(fan);
                                    }
                                    if (Objects.equals(action, "turn_off")){
                                        publish(FEEDS[1], "0");
                                        Log log = new Log();
                                        log.setLogId(UUID.randomUUID().toString());
                                        log.setLEDColor(null);
                                        log.setFanSpeed(Double.valueOf("0"));
                                        log.setLEDStatus(null);
                                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                                        logRepo.save(log);

                                        Device fan = deviceRepo.getDeviceByDeviceId("FAN_1").orElse(null);
                                        assert fan != null;
                                        fan.setFanSpeed(Double.valueOf("0"));
                                        fan.setStatus("OFF");
                                        deviceRepo.save(fan);
                                    }
                                }

                            }
                            case "led" -> {
                                if (operators.get(condition).test(sensorValue, conditionValue)) {
                                    if (Objects.equals(action, "turn_on")) {
                                        String color = "#ff0000";
                                        publish("led", "1");
                                        publish("led-color", color);

                                        Log log = new Log();
                                        log.setLogId(UUID.randomUUID().toString());
                                        log.setLEDColor(color);
                                        log.setFanSpeed(null);
                                        log.setLEDStatus("ON");
                                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                                        logRepo.save(log);

                                        Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);
                                        assert led != null;
                                        led.setLedColor(color);
                                        led.setStatus("ON");
                                        deviceRepo.save(led);
                                    }
                                    if (Objects.equals(action, "turn_off")) {
                                        String color = "#000000";
                                        publish("led", "0");
                                        publish("led-color", color);

                                        Log log = new Log();
                                        log.setLogId(UUID.randomUUID().toString());
                                        log.setLEDColor(color);
                                        log.setFanSpeed(null);
                                        log.setLEDStatus("OFF");
                                        log.setTimestamp(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
                                        logRepo.save(log);

                                        Device led = deviceRepo.getDeviceByDeviceId("LED_1").orElse(null);
                                        assert led != null;
                                        led.setLedColor(color);
                                        led.setStatus("OFF");
                                        deviceRepo.save(led);
                                    }
                                }
                            }
                        }
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

    public synchronized void publish(String feed, String payload) {
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
    private void syncScheduler() {
        clientListenerAndUpdate(); // Sau đó mới chạy task 2
        checkSchedule(); // Chạy task 1

    }

    private void clientListenerAndUpdate(){
        System.out.println("Retrieve from client");
        List<Device> devices = deviceRepo.findAll();
        for(Device device : devices){
            LocalDateTime date = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            if (device.getFanSpeed() != null && device.getEditFlag()){
                publish("fan",Double.toString(device.getFanSpeed()));
                device.setStatus(device.getFanSpeed() == 0 ? "OFF" : "ON");

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
                device.setStatus(Objects.equals(color, "#000000") ? "OFF" : "ON");
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

    private void checkSchedule(){
        System.out.println("Retrieve schedule");
        List<Device> devices = deviceRepo.findAll();
        for (Device device : devices){
            if (Objects.equals("FAN_1", device.getDeviceId())){
                Schedule fanSchedule = device.getSchedule();
                // getValue trả về giá trị từ t2 -> CN: 1 -> 7
                int today = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).getDayOfWeek().getValue() - 1;
                boolean[] effectiveDays = fanSchedule.getWeekdaysRepeat();

                if (fanSchedule.getTime() == null){
                    continue;
                }
                String[] trimmed = fanSchedule.getTime().split(":");
                LocalTime scheduleTime = LocalTime.of(Integer.parseInt(trimmed[0]) + 7, Integer.parseInt(trimmed[1]), 0);

//                scheduleTime = scheduleTime.plusHours(7);


                LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
//                nowTime = nowTime.minusMinutes(3);
//                nowTime = nowTime.plusSeconds(20);

//                System.out.println(fanSchedule.toString());
                System.out.println(scheduleTime.toString());
                System.out.println(nowTime);
//                System.out.println(effectiveDays[today]);
//                System.out.println(fanSchedule.isRepeat());
//                System.out.println(Fanruns.get(today));

                if (effectiveDays[today] && (fanSchedule.isRepeat() || (Fanruns.get(today) == 0 && !fanSchedule.isRepeat()))){
//                    Fanruns.merge(today, 1, Integer::sum);
                    long timeDiff = Duration.between(scheduleTime, nowTime).getSeconds();
                    System.out.println(timeDiff);
                    if ( timeDiff >= 0 && timeDiff <= 60 ){
//                        if (nowTime.isAfter(scheduleTime) && FansRunToday == 0){
//                            FansRunToday++;
                            if(Objects.equals("set_value", fanSchedule.getSelectAction())){
                                device.setStatus(Double.parseDouble(fanSchedule.getActionValue()) != 0 ? "ON" : "OFF");
                                device.setFanSpeed(Double.parseDouble(fanSchedule.getActionValue()));
                                publish(FEEDS[1], fanSchedule.getActionValue());
                            }
                            if(Objects.equals("turn_off", fanSchedule.getSelectAction())) {
                                device.setStatus("OFF");
                                device.setFanSpeed(Double.valueOf("0"));
                                publish(FEEDS[1], "0");
                            }
                            if(Objects.equals("turn_on", fanSchedule.getSelectAction())){
                                device.setStatus("ON");
                                device.setFanSpeed(Double.valueOf("50"));
                                publish(FEEDS[1], "50");
                            }
//                        }
                    }
                }
                device.setSchedule(fanSchedule);
            }
            if (Objects.equals("LED_1", device.getDeviceId())){
                Schedule ledSchedule = device.getSchedule();

                int today = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).getDayOfWeek().getValue() - 1;
                boolean[] effectiveDays = ledSchedule.getWeekdaysRepeat();

                if (ledSchedule.getTime() == null){
                    continue;
                }

                String[] trimmed = ledSchedule.getTime().split(":");
                LocalTime scheduleTime = LocalTime.of(Integer.parseInt(trimmed[0]) + 7, Integer.parseInt(trimmed[1]), 0);


                LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
//                nowTime = nowTime.minusMinutes(3);
//                nowTime = nowTime.plusSeconds(20);
//                nowTime = nowTime.minusSeconds(30);

                // Nếu như hôm đó có hiệu lực và được lặp lại hoặc không được lặp lại nhưng chưa chạy lần nào
                if (effectiveDays[today] && (ledSchedule.isRepeat() || (Ledruns.get(today) == 0 && !ledSchedule.isRepeat()))){
//                    Ledruns.merge(today, 1, Integer::sum);
                    // Nếu trong ngày chưa chạy lần nào và hiện tại đã sau tgian lên lịch
                    long timeDiff = Duration.between(scheduleTime, nowTime).getSeconds();
                    if ( timeDiff >= 0 && timeDiff <= 60 ){
//                        LedsRunToday++;
                        if(Objects.equals("set_value", ledSchedule.getSelectAction())){
                            String color = ledSchedule.getActionValue() == null ? "#ff0000" : ledSchedule.getActionValue();
                            device.setStatus(Objects.equals(color, "#000000") ? "OFF" : "ON");
                            device.setLedColor(color);
                            publish(FEEDS[5], Objects.equals(color, "#000000") ? "0" : "1");
                            publish(FEEDS[0], color);
                        }
                        if(Objects.equals("turn_off", ledSchedule.getSelectAction())) {
                            device.setStatus("OFF");
                            device.setLedColor("#000000");
                            publish(FEEDS[5], "0");
                            publish(FEEDS[0], "#000000");
                        }
                        if(Objects.equals("turn_on", ledSchedule.getSelectAction())){
                            device.setStatus("ON");
                            device.setLedColor("#ff0000");
                            publish(FEEDS[5], "1");
                            publish(FEEDS[0], "#ff0000");
                        }
                    }
                }
                device.setSchedule(ledSchedule);
            }
        }
        deviceRepo.saveAll(devices);
    }
}
