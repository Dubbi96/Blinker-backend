package com.blinker.atom.controller;

import com.blinker.atom.dto.sensor.SensorExecutionInstanceResponseDto;
import com.blinker.atom.service.scheduled.AppUserSensorGroupService;
import com.blinker.atom.service.scheduled.ExecutionInstanceService;
import com.blinker.atom.service.scheduled.SensorLogSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class ScheduledServiceController {
    private final SensorLogSchedulerService sensorLogSchedulerService;
    private final AppUserSensorGroupService appUserSensorGroupService;
    private final ExecutionInstanceService executionInstanceService;

    @PostMapping("/sensor-log/start")
    @Operation(summary = "보유한 모든 SensorGroup의 로그 수집", description = "⛔️ 멀티 쓰레드 환경에서 동작하므로 사용에 주의 요함")
    public String sensorLogStartScheduler() {
        sensorLogSchedulerService.fetchAndSaveSensorLogs();
        return "Sensor Log 스케줄링이 시작";
    }

    @PostMapping("/sensor/start")
    @Operation(summary = "Sensor 정보 업데이트", description = "⛔️ 비동기 호출으로 사용에 주의 요함")
    public String sensorStartScheduler() {
        sensorLogSchedulerService.updateSensorFromSensorLogs();
        return "Sensor 스케줄링 시작";
    }

    @PostMapping("/admin/start")
    @Operation(summary = "Admin 유저에게 모든 SensorGroup 할당", description = "⛔️ 비동기 호출으로 사용에 주의 요함")
    public String adminSchedulerStartScheduler() {
        appUserSensorGroupService.updateAdminSensorGroups();
        return "ADMIN 유저 업데이트 스케줄링 시작";
    }

    @GetMapping("/sensor-execution-log/start")
    public List<SensorExecutionInstanceResponseDto> sensorExecutionLogStartScheduler() {
        return executionInstanceService.fetchSensorExecutionLogs();
    }
}
