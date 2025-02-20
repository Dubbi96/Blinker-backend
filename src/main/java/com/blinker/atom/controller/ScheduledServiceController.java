package com.blinker.atom.controller;

import com.blinker.atom.service.scheduled.AppUserSensorGroupService;
import com.blinker.atom.service.scheduled.SensorLogSchedulerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class ScheduledServiceController {
    private final SensorLogSchedulerService sensorLogSchedulerService;
    private final AppUserSensorGroupService appUserSensorGroupService;

    @PostMapping("/sensor-log/start")
    public String sensorLogStartScheduler() {
        sensorLogSchedulerService.fetchAndSaveSensorLogs();
        return "✅ Sensor Log 스케줄링이 시작되었습니다.";
    }

    /*@PostMapping("/sensor-log/stop")
    public String sensorLogStopScheduler() {
        sensorLogSchedulerService.stopScheduler();
        return "⛔ Sensor Log 스케줄링이 중지되었습니다.";
    }*/

    @PostMapping("/admin/start")
    public String adminSchedulerStartScheduler() {
        appUserSensorGroupService.updateAdminSensorGroups();
        return "✅ ADMIN 유저 업데이트 스케줄링이 시작되었습니다.";
    }

    /*@PostMapping("/admin/stop")
    public String adminSchedulerStopScheduler() {
        appUserSensorGroupService.stopScheduler();
        return "⛔ ADMIN 유저 업데이트 스케줄링이 중지되었습니다.";
    }*/
}
