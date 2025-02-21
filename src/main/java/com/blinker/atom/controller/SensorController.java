package com.blinker.atom.controller;

import com.blinker.atom.config.security.LoginAppUser;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.dto.sensor.SensorDetailResponseDto;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import com.blinker.atom.dto.sensor.SensorLogResponseDto;
import com.blinker.atom.dto.sensor.UnregisteredSensorGroupResponseDto;
import com.blinker.atom.service.sensor.SensorGroupService;
import com.blinker.atom.service.sensor.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;
    private final SensorGroupService sensorGroupService;

    @GetMapping("/groups")
    @Operation(summary = "사용자의 모든 센서 조회",description = "<b>@LoginAppUser</b> 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회 <br> <b>📌 정렬 기준:</b> <br> 1. 센서 그룹 ID 오름차순 <br> 2. 센서 groupPositionNumber 오름차순")
    public List<SensorGroupResponseDto> getSensorGroups(@LoginAppUser AppUser appUser) {
        return sensorGroupService.getSensorGroups(appUser);
    }

    @GetMapping("/groups/{appUserId}")
    @Operation(summary = "appUserId를 기입한 사용자의 모든 센서 조회 ⭐️Admin 전용", description = "<b>appUserId에 해당하는 AppUser가 보유한 SensorGroup의 모든 정보 조회</b> <br> <b>📌 정렬 기준:</b> <br> 1. 센서 그룹 ID 오름차순 <br> 2. 센서 groupPositionNumber 오름차순")
    public List<SensorGroupResponseDto> getSensorGroups(@PathVariable("appUserId") Long appUserId) {
        return sensorGroupService.getSensorGroupsByAppUserId(appUserId);
    }

    @GetMapping("/groups/unregistered")
    @Operation(summary = "미등록 된 sensorGroupId 조회 ⭐️Admin 전용", description = "<b>미등록 된 센서 목록 조회</b> <br> 미등록은 ADMIN 계정을 제외한 User 계정에 등록되지 않은 상태 <br> ***@Value : longitude, latitude***는 해당 SensorGroup의 Master센서의 위치만 반환 <br> 만약 해당 SensorGroup에 아무 Sensor도 없을 경우 ***@Value : longitude, latitude***는 ***null***로 반환")
    public List<UnregisteredSensorGroupResponseDto> getSensorDetail() {
        return sensorGroupService.getUnregisteredSensorGroups();
    }

    @GetMapping("/{sensorId}/logs")
    @Operation(summary = "sensorId에 관련된 모든 로그 정보 목록 조회", description = "<b>sensorId에 해당하는 로그 정보 목록 조회</b> <br> <b>📌 정렬 기준:</b> 로그 발생 시간 역순 <br> <b>🗓️ 날짜 필터:</b> <br> 1. <b>@param year</b> 만 입력 시 → 해당 연도에 해당하는 로그 출력 <br> 2. <b>@param year, month</b> 입력 시 → 해당 월에 해당하는 로그 출력 <br> 3. <b>@param year, month, day</b> 입력 시 → 해당 일에 해당하는 로그 출력 <br> 4. 그 외 → 모든 로그 출력")
    public List<SensorLogResponseDto> getSensorLogsBySensorDeviceNumber(
                @PathVariable("sensorId") Long sensorId,
                @LoginAppUser AppUser appUser,
                @RequestParam(required = false) Integer year,
                @RequestParam(required = false) Integer month,
                @RequestParam(required = false) Integer day
    ) {
        return sensorService.getSensorLogsBySensorId(sensorId, appUser, year, month, day);
    }

    @GetMapping("/{sensorId}/detail")
    @Operation(summary = "sensorId와 관련된 상세 정보 조회", description = "<b>fault information</b>이 하나라도 <b>true</b>일 경우 <b>status = 오류</b>로 표현, 기기 위치는 <b>latitude, longitude</b>로 전달")
    public SensorDetailResponseDto getSensorDetail(@PathVariable("sensorId") Long sensorId, @LoginAppUser AppUser appUser) {
        return sensorService.getSensorDetailBySensorId(sensorId, appUser);
    }

}
