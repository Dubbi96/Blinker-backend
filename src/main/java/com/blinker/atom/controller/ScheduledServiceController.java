package com.blinker.atom.controller;

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

    @PostMapping("/start")
    public String startScheduler() {
        sensorLogSchedulerService.startScheduler();
        return "✅ Sensor Log 스케줄링이 시작되었습니다.";
    }

    @PostMapping("/stop")
    public String stopScheduler() {
        sensorLogSchedulerService.stopScheduler();
        return "⛔ Sensor Log 스케줄링이 중지되었습니다.";
    }
}
