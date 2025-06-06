package com.blinker.atom.controller;

import com.blinker.atom.dto.sensor.SensorExecutionInstanceResponseDto;
import com.blinker.atom.service.scheduled.AppUserSensorGroupService;
import com.blinker.atom.service.scheduled.ExecutionInstanceService;
import com.blinker.atom.service.scheduled.SensorLogSchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public String sensorLogScheduler() {
        sensorLogSchedulerService.fetchAndSaveSensorLogs();
        return "Sensor Log 스케줄링이 시작";
    }

    @PostMapping("/sensor/start")
    @Operation(summary = "Sensor 정보 업데이트", description = "⛔️ 비동기 호출으로 사용에 주의 요함")
    public String sensorScheduler() {
        sensorLogSchedulerService.updateSensorFromSensorLogs();
        return "Sensor 스케줄링 시작";
    }

    @PostMapping("/admin/start")
    @Operation(summary = "Admin 유저에게 모든 SensorGroup 할당", description = "⛔️ 비동기 호출으로 사용에 주의 요함")
    public String adminSchedulerScheduler() {
        appUserSensorGroupService.updateAdminSensorGroups();
        return "ADMIN 유저 업데이트 스케줄링 시작";
    }

    @GetMapping("/sensor-execution-log/start")
    @Operation(summary = "전체 환경에서 활용된 execution log detail 전부 조회", description = "확인용 API")
    public List<SensorExecutionInstanceResponseDto> sensorExecutionLogStartScheduler() {
        return executionInstanceService.fetchSensorExecutionLogs();
    }

    @PutMapping("/sensor-address/start")
    @Operation(summary = "보유한 모든 Sensor의 주소지 업데이트", description = "⛔️ 멀티 쓰레드 환경에서 동작하므로 사용에 주의 요함")
    public String sensorAddressUpdateScheduler() {
        sensorLogSchedulerService.updateSensorAddress();
        return "Sensor 주소정보 업데이트 시작";
    }

    @PatchMapping("/sensor-roll-back/start")
    @Operation(summary = "보유한 모든 Sensor 정보 rollback", description = "⛔️ 멀티 쓰레드 환경에서 동작하므로 사용에 주의 요함")
    public String rollbackAllSensors() {
        sensorLogSchedulerService.rollbackSensors();
        return "Sensor 정보 rollback 시작";
    }

    @DeleteMapping("/archive-sensor-log")
    @Operation(summary = "센서 로그 아카이브화", description = "⛔️ 로그 삭제 이루어짐. 일일 1회만 호출할 것.")
    public String archiveSensorLog() {
        sensorLogSchedulerService.archiveLogsBySensorDeviceNumber();
        return "Sensor Log 이관";
    }
}
