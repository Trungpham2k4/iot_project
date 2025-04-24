package com.example.iot_project.service;

import com.example.iot_project.model.Device;
import com.example.iot_project.model.Schedule;
import com.example.iot_project.repository.DeviceRepo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ScheduleService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String[] FEEDS = {dotenv.get("ADA_FRUIT_LED_COLOR"), dotenv.get("ADA_FRUIT_FAN"),
            dotenv.get("ADA_FRUIT_HUMID"), dotenv.get("ADA_FRUIT_LIGHT"), dotenv.get("ADA_FRUIT_TEMP"), dotenv.get("ADA_FRUIT_LED")};
    private static final String[] DEVICE_NAME = {"FAN_1", "LED_1"};
    private final DeviceRepo deviceRepo;

    private final ArrayList<Integer> activeFanSchedule = new ArrayList<>();
    private final AtomicBoolean updatePrevFan = new AtomicBoolean(true);
    private final AtomicInteger fanValue = new AtomicInteger(0);

    private final ArrayList<Integer> activeLedSchedule = new ArrayList<>();
    private final AtomicBoolean updatePrevLed = new AtomicBoolean(true);
    private String ledValue = "#000000";


    // Phụ thuộc vòng lặp giữa 2 bean: Lỗi này xảy ra do sự phụ thuộc vòng (circular dependency) giữa hai bean trong Spring,
    // cụ thể là giữa AdafruitRequestManagerService và ScheduleService.
    // Điều này có nghĩa là AdafruitRequestManagerService đang phụ thuộc vào ScheduleService và ngược lại,
    // tạo thành một vòng lặp không thể giải quyết trong quá trình khởi tạo các bean.
    private final AdafruitRequestManagerService adafruitRequestManagerService;

    Map<Integer, Integer> Fanruns = new HashMap<>();
    Map<Integer, Integer> Ledruns = new HashMap<>();

    @Autowired
    public ScheduleService(DeviceRepo deviceRepo,
                           AdafruitRequestManagerService adafruitRequestManagerService) {
        this.deviceRepo = deviceRepo;
        this.adafruitRequestManagerService = adafruitRequestManagerService;
    }

    @PostConstruct
    private void init(){
//        Fanruns.put(0,0);
//        Fanruns.put(1,0);
//        Fanruns.put(2,0);
//        Fanruns.put(3,0);
//        Fanruns.put(4,0);
//        Fanruns.put(5,0);
//        Fanruns.put(6,0);
//
//        Ledruns.put(0,0);
//        Ledruns.put(1,0);
//        Ledruns.put(2,0);
//        Ledruns.put(3,0);
//        Ledruns.put(4,0);
//        Ledruns.put(5,0);
//        Ledruns.put(6,0);
    }

    public void checkSchedule() throws InterruptedException {
        System.out.println("Retrieve schedule");
        List<Device> devices = deviceRepo.findAll();
        int today = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).getDayOfWeek().getValue() - 1;
//        if (prevDay == today - 2 || (today == 1 && prevDay == 6)){
//            prevDay = today - 1;
//            Fanruns.merge(prevDay, 1, Integer::sum);
//            Ledruns.merge(prevDay, 1, Integer::sum);
//        }
//        if (today == 0 && prevDay == 5){
//            prevDay = 6;
//            Fanruns.merge(prevDay, 1, Integer::sum);
//            Ledruns.merge(prevDay, 1, Integer::sum);
//        }
        for (Device device : devices){
            if (Objects.equals("FAN_1", device.getDeviceId())){
                List<Schedule> fanSchedules = device.getSchedule();
                if (fanSchedules.isEmpty()){
                    continue;
                }
                fanSchedules
                        .stream().filter(s -> ZonedDateTime.parse(s.getTime())
                                .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime().isBefore(LocalTime.now())).collect(Collectors.toList())
                        .sort(Comparator.comparing(s -> ZonedDateTime.parse(s.getTime())
                        .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime()));
                ///  Ý tưởng mới, Dùng array lưu trạng thái của Schedule, cái nào hoạt động và k hoạt động
                for (int i = 0; i < fanSchedules.size(); i++){
                    // getValue trả về giá trị từ t2 -> CN: 1 -> 7
                    boolean[] effectiveDays = fanSchedules.get(i).getWeekdaysRepeat();

                    String action = fanSchedules.get(i).getSelectAction();

                    ZonedDateTime startUtcTime = ZonedDateTime.parse(fanSchedules.get(i).getTime());
                    ZonedDateTime endUtcTime = ZonedDateTime.parse(fanSchedules.get(i).getTo());

                    LocalTime start = startUtcTime.withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime();
                    LocalTime end = endUtcTime.withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime();


                    LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));


                    System.out.println(start);
                    System.out.println(nowTime);
                    System.out.println(end);

                    long startPeriod = Duration.between(nowTime, start).getSeconds();
                    long endPeriod = Duration.between(end, nowTime).getSeconds();

                    if (startPeriod >= 0 && startPeriod < 10){
                        activeFanSchedule.add(i);
                        if(updatePrevFan.get()){
                            updatePrevFan.set(false);
                            fanValue.set(device.getFanSpeed());
                        }
                    } else if (nowTime.isAfter(start) && nowTime.isBefore(end)){
                        if(Objects.equals("set_value", action)){
                            device.setStatus(Integer.parseInt(fanSchedules.get(i).getActionValue()) != 0 ? "ON" : "OFF");
                            device.setFanSpeed(Integer.parseInt(fanSchedules.get(i).getActionValue()));
                            adafruitRequestManagerService.publish(FEEDS[1], fanSchedules.get(i).getActionValue());
                        }
                        if(Objects.equals("turn_off", action)) {
                            device.setStatus("OFF");
                            device.setFanSpeed(Integer.valueOf("0"));
                            adafruitRequestManagerService.publish(FEEDS[1], "0");
                        }
                        if(Objects.equals("turn_on", action)){
                            device.setStatus("ON");
                            device.setFanSpeed(Integer.valueOf("50"));
                            adafruitRequestManagerService.publish(FEEDS[1], "50");
                        }
                    }else if(endPeriod >= 0 && endPeriod < 10){
                        activeFanSchedule.remove(Integer.valueOf(i));
                        int res;
                        if (activeFanSchedule.isEmpty()){
                            updatePrevFan.set(true);
                            res = fanValue.get();
                            if(res == 0){
                                device.setStatus("OFF");
                            }else{
                                device.setStatus("ON");
                            }
                            device.setFanSpeed(res);
                            adafruitRequestManagerService.publish(FEEDS[1], ""+res);
                        }
                    }

                }
            }
            if (Objects.equals("LED_1", device.getDeviceId())){
                List<Schedule> ledSchedules = device.getSchedule();
                if (ledSchedules.isEmpty()){
                    continue;
                }
                ledSchedules
                        .stream().filter(s -> ZonedDateTime.parse(s.getTime())
                                .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime().isBefore(LocalTime.now())).collect(Collectors.toList())
                        .sort(Comparator.comparing(s -> ZonedDateTime.parse(s.getTime())
                                .withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime()));
                for (int i = 0; i < ledSchedules.size(); i++){
                    boolean[] effectiveDays = ledSchedules.get(i).getWeekdaysRepeat();

                    String action = ledSchedules.get(i).getSelectAction();

                    ZonedDateTime startUtcTime = ZonedDateTime.parse(ledSchedules.get(i).getTime());
                    ZonedDateTime endUtcTime = ZonedDateTime.parse(ledSchedules.get(i).getTo());

                    LocalTime start = startUtcTime.withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime();
                    LocalTime end = endUtcTime.withZoneSameInstant(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime();



                    LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
//                    System.out.println(start);
//                    System.out.println(nowTime);
//                    System.out.println(end);

                    long startPeriod = Duration.between(nowTime, start).getSeconds();
                    long endPeriod = Duration.between(end, nowTime).getSeconds();

                    if (startPeriod >= 0 && startPeriod < 10){
                        activeLedSchedule.add(i);
                        if (updatePrevLed.get()){
                            updatePrevLed.set(false);
                            ledValue = device.getLedColor();
                        }
                    } else if (nowTime.isAfter(start) && nowTime.isBefore(end)){
                        if(Objects.equals("set_value", action)){
                            String color = ledSchedules.get(i).getActionValue();
                            device.setStatus(Objects.equals(color, "#000000") ? "OFF" : "ON");
                            device.setLedColor(color);
                            adafruitRequestManagerService.publish(FEEDS[5], Objects.equals(color, "#000000") ? "0" : "1");
                            adafruitRequestManagerService.publish(FEEDS[0], color);
                        }
                        if(Objects.equals("turn_off", action)) {
                            device.setStatus("OFF");
                            device.setLedColor("#000000");
                            adafruitRequestManagerService.publish(FEEDS[5], "0");
                            adafruitRequestManagerService.publish(FEEDS[0], "#000000");
                        }
                        if(Objects.equals("turn_on", action)){
                            device.setStatus("ON");
                            device.setLedColor("#ff0000");
                            adafruitRequestManagerService.publish(FEEDS[5], "1");
                            adafruitRequestManagerService.publish(FEEDS[0], "#ff0000");
                        }
                    }else if(endPeriod >= 0 && endPeriod < 10){
                        activeLedSchedule.remove(Integer.valueOf(i));
                        String res;
                        if (activeLedSchedule.isEmpty()){
                            updatePrevLed.set(true);
                            res = ledValue;
                            if(Objects.equals(res, "#000000")){
                                device.setStatus("OFF");
                                adafruitRequestManagerService.publish(FEEDS[5], "0");
                            }else{
                                device.setStatus("ON");
                                adafruitRequestManagerService.publish(FEEDS[5], "1");
                            }
                            device.setLedColor(res);
                            adafruitRequestManagerService.publish(FEEDS[0], res);
                        }
                    }
                }
            }
        }
        deviceRepo.saveAll(devices);
    }
}
