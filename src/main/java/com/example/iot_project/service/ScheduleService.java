package com.example.iot_project.service;

import com.example.iot_project.model.Device;
import com.example.iot_project.model.Schedule;
import com.example.iot_project.repository.DeviceRepo;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ScheduleService {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String[] FEEDS = {dotenv.get("ADA_FRUIT_LED_COLOR"), dotenv.get("ADA_FRUIT_FAN"),
            dotenv.get("ADA_FRUIT_HUMID"), dotenv.get("ADA_FRUIT_LIGHT"), dotenv.get("ADA_FRUIT_TEMP"), dotenv.get("ADA_FRUIT_LED")};
    private static final String[] DEVICE_NAME = {"FAN_1", "LED_1"};
    private final DeviceRepo deviceRepo;


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
    }

    public void checkSchedule(){
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
                Schedule fanSchedule = device.getSchedule();
                if (fanSchedule == null){
                    continue;
                }
                // getValue trả về giá trị từ t2 -> CN: 1 -> 7
                boolean[] effectiveDays = fanSchedule.getWeekdaysRepeat();

//                if (fanSchedule.getTime() == null){
//                    continue;
//                }
                String[] trimmed = fanSchedule.getTime().split(":");
                LocalTime scheduleTime = LocalTime.of(Integer.parseInt(trimmed[0]) + 7, Integer.parseInt(trimmed[1]), 0);



                LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

//                System.out.println(fanSchedule.toString());
//                System.out.println(scheduleTime.toString());
//                System.out.println(nowTime);
//                System.out.println(effectiveDays[today]);
//                System.out.println(fanSchedule.isRepeat());
//                System.out.println(Fanruns.get(today));

                if (effectiveDays[today] && (fanSchedule.isRepeat() || (Fanruns.get(today) == 0 && !fanSchedule.isRepeat()))){
                    long timeDiff = Duration.between(scheduleTime, nowTime).getSeconds();
                    System.out.println(timeDiff);
                    if ( timeDiff >= 0 && timeDiff < 10 ){
                        if(Objects.equals("set_value", fanSchedule.getSelectAction())){
                            device.setStatus(Integer.parseInt(fanSchedule.getActionValue()) != 0 ? "ON" : "OFF");
                            device.setFanSpeed(Integer.parseInt(fanSchedule.getActionValue()));
                            adafruitRequestManagerService.publish(FEEDS[1], fanSchedule.getActionValue());
                        }
                        if(Objects.equals("turn_off", fanSchedule.getSelectAction())) {
                            device.setStatus("OFF");
                            device.setFanSpeed(Integer.valueOf("0"));
                            adafruitRequestManagerService.publish(FEEDS[1], "0");
                        }
                        if(Objects.equals("turn_on", fanSchedule.getSelectAction())){
                            device.setStatus("ON");
                            device.setFanSpeed(Integer.valueOf("50"));
                            adafruitRequestManagerService.publish(FEEDS[1], "50");
                        }
//                        }
                    }
                }
                device.setSchedule(fanSchedule);
            }
            if (Objects.equals("LED_1", device.getDeviceId())){
                Schedule ledSchedule = device.getSchedule();
                if (ledSchedule == null){
                    continue;
                }

                boolean[] effectiveDays = ledSchedule.getWeekdaysRepeat();

//                if (ledSchedule.getTime() == null){
//                    continue;
//                }

                String[] trimmed = ledSchedule.getTime().split(":");
                LocalTime scheduleTime = LocalTime.of(Integer.parseInt(trimmed[0]) + 7, Integer.parseInt(trimmed[1]), 0);


                LocalTime nowTime = LocalTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

                // Nếu như hôm đó có hiệu lực và được lặp lại hoặc không được lặp lại nhưng chưa chạy lần nào
                if (effectiveDays[today] && (ledSchedule.isRepeat() || (Ledruns.get(today) == 0 && !ledSchedule.isRepeat()))){
                    long timeDiff = Duration.between(scheduleTime, nowTime).getSeconds();
//                    System.out.println(timeDiff);
                    if ( timeDiff >= 0 && timeDiff < 10 ){
                        if(Objects.equals("set_value", ledSchedule.getSelectAction())){
                            String color = ledSchedule.getActionValue();
                            device.setStatus(Objects.equals(color, "#000000") ? "OFF" : "ON");
                            device.setLedColor(color);
                            adafruitRequestManagerService.publish(FEEDS[5], Objects.equals(color, "#000000") ? "0" : "1");
                            adafruitRequestManagerService.publish(FEEDS[0], color);
                        }
                        if(Objects.equals("turn_off", ledSchedule.getSelectAction())) {
                            device.setStatus("OFF");
                            device.setLedColor("#000000");
                            adafruitRequestManagerService.publish(FEEDS[5], "0");
                            adafruitRequestManagerService.publish(FEEDS[0], "#000000");
                        }
                        if(Objects.equals("turn_on", ledSchedule.getSelectAction())){
                            device.setStatus("ON");
                            device.setLedColor("#ff0000");
                            adafruitRequestManagerService.publish(FEEDS[5], "1");
                            adafruitRequestManagerService.publish(FEEDS[0], "#ff0000");
                        }
                    }
                }
                device.setSchedule(ledSchedule);
            }
        }
        deviceRepo.saveAll(devices);
    }
}
