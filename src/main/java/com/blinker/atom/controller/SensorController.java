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
import org.springframework.web.bind.annotation.PathVariable;
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
    @Operation(summary = "사용자의 모든 센서 조회", description = "@LoginAppUser 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회 📌 정렬 기준 : (1) 센서 그룹 ID 오름차순 (2) 센서 groupPositionNumber 오름차순")
    public List<SensorGroupResponseDto> getSensorGroups(@LoginAppUser AppUser appUser) {
        return sensorGroupService.getSensorGroups(appUser);
    }

    @GetMapping("/groups/{appUserId}")
    @Operation(summary = "appUserId를 기입한 사용자의 모든 센서 조회 ⭐️Admin 전용", description = "appUserId에 해당하는 AppUser가 보유한 SensorGroup의 모든 정보 조회 📌 정렬 기준 : (1) 센서 그룹 ID 오름차순 (2) 센서 groupPositionNumber 오름차순")
    public List<SensorGroupResponseDto> getSensorGroups(@PathVariable("appUserId") Long appUserId) {
        return sensorGroupService.getSensorGroupsByAppUserId(appUserId);
    }

}