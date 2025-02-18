package com.blinker.atom.controller;

import com.blinker.atom.config.security.LoginAppUser;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import com.blinker.atom.service.appuser.AppUserService;
import com.blinker.atom.service.sensor.SensorGroupService;
import com.blinker.atom.service.sensor.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;
    private final SensorGroupService sensorGroupService;

    @GetMapping("/groups")
    @Operation(summary = "사용자의 모든 센서 조회", description = "@LoginAppUser 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회")
    public List<SensorGroupResponseDto> getSensorGroups(@LoginAppUser AppUser appUser) {
        return sensorGroupService.getSensorGroups(appUser);
    }

    /**
     * 신호기 목록 조회 API
     * @return 신호기 데이터 목록
     */
    /*@GetMapping
    public ResponseEntity<List<SensorDto>> getAllSensors() {
        log.info("전체 조회 : /api/sensors");
        return ResponseEntity.ok(sensorService.getAllSensors());
    }*/

    /**
     * 신호기 목록 조회 API
     * @return 신호기 데이터 목록
     */
    /*@GetMapping("/detail")
    public ResponseEntity<List<Map<String, Object>>> getSensorDetailList() {
        log.info("디테일 조회 : /api/sensors/detail");
        return ResponseEntity.ok(sensorService.getAllSensorDetail());
    }*/

}