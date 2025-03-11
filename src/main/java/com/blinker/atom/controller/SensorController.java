package com.blinker.atom.controller;

import com.blinker.atom.config.security.LoginAppUser;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.dto.sensor.*;
import com.blinker.atom.dto.thingplug.SensorUpdateRequestDto;
import com.blinker.atom.service.sensor.SensorGroupService;
import com.blinker.atom.service.sensor.SensorService;
import com.blinker.atom.service.thingplug.ThingPlugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final SensorService sensorService;
    private final SensorGroupService sensorGroupService;
    private final ThingPlugService thingPlugService;

    @GetMapping("/groups")
    @Operation(summary = "사용자의 모든 센서 조회",description = "<b>@LoginAppUser</b> 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회 <br><b>🗓️ 장애센서 필터: </b><br> 1. onlyFaulty = true로 보낸다면 장애 센서만 조회<br> 2. onlyFaulty = false혹은 보내지 않는다면 전체 조회 <br> <b>📌 정렬 기준:</b> <br> 1. SensorGroup의 Order순 <br> 2. 센서 groupPositionNumber 오름차순 <br> ")
    public List<SensorGroupResponseDto> getSensorGroups(@LoginAppUser AppUser appUser, @RequestParam(defaultValue = "false") boolean onlyFaulty) {
        return sensorGroupService.getSensorGroups(appUser, onlyFaulty);
    }

    @GetMapping("/groups/{appUserId}")
    @Operation(summary = "appUserId를 기입한 사용자의 모든 센서 조회 ⭐️Admin 전용", description = "<b>appUserId에 해당하는 AppUser가 보유한 SensorGroup의 모든 정보 조회</b><br> <b>🗓️ 장애센서 필터: </b><br> 1. onlyFaulty = true로 보낸다면 장애 센서만 조회<br> 2. onlyFaulty = false혹은 보내지 않는다면 전체 조회 <br> <b>📌 정렬 기준:</b> <br> 1. 센서 그룹 ID 오름차순 <br> 2. 센서 groupPositionNumber 오름차순")
    public List<SensorGroupResponseDto> getSensorGroups(@PathVariable("appUserId") Long appUserId, @RequestParam(defaultValue = "false") boolean onlyFaulty) {
        return sensorGroupService.getSensorGroupsByAppUserId(appUserId, onlyFaulty);
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

    @PutMapping("/{sensorGroupId}")
    @Operation(summary = "단일 sensor 정보 업데이트 요청", description = "<b>단일 sensor 정보 업데이트 요청</b>")
    public String updateSensorToThingPlug(
            @Parameter(example = "0000102140ca63fffe1df1ce")
            @PathVariable("sensorGroupId") String sensorGroupId,
            @RequestBody SensorUpdateRequestDto content
    ) {
        log.info("Received request to create contentInstance: content={}",
                content);
        return thingPlugService.updateSensorToThingPlug(sensorGroupId, content);
    }

    @PatchMapping("/memo/{sensorId}")
    @Operation(summary = "단일 sensor memo 생성 / 수정", description = "<b>단일 sensor 메모</b> <br> 해당 AppUser에게 Sensor 메모가 존재한다면 <b>수정</b> <br> 해당 AppUser에게 Sensor 메모가 존재하지 않는다면<b> 신규 생성</b>")
    public void updateOrCreate(@LoginAppUser AppUser appUser,
                               @PathVariable("sensorId") Long sensorId,
                               @RequestBody SensorMemoRequestDto sensorMemoRequestDto
    ) {
        sensorService.updateOrCreateSensorMemo(appUser, sensorId, sensorMemoRequestDto);
    }
}
