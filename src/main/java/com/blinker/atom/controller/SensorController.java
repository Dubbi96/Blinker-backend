package com.blinker.atom.controller;

import com.blinker.atom.config.security.LoginAppUser;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.dto.sensor.*;
import com.blinker.atom.dto.thingplug.SensorUpdateRequestDto;
import com.blinker.atom.service.scheduled.SensorLogSchedulerService;
import com.blinker.atom.service.sensor.SensorGroupService;
import com.blinker.atom.service.sensor.SensorService;
import com.blinker.atom.service.thingplug.ThingPlugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
    private final SensorLogSchedulerService sensorLogSchedulerService;

    @GetMapping("/groups")
    @Operation(summary = "사용자의 모든 센서 조회",description = "<b>@LoginAppUser</b> 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회 <br><b>🗓️ 장애센서 필터: </b><br> 1. onlyFaulty = true로 보낸다면 장애 센서만 조회<br> 2. onlyFaulty = false혹은 보내지 않는다면 전체 조회 <br> <b>📌 정렬 기준:</b> <br> 1. SensorGroup의 Order순 <br> 2. 센서 groupPositionNumber 오름차순 <br><br> 2025-03-20 update : ***SensorDto.needUpdate***는 생성된 후 3일 이내 update되지 않았거나 최근 update 이력이 3일내로 없는 센서에게 ***true***로 반환")
    public List<SensorGroupResponseDto> getSensorGroups(@LoginAppUser AppUser appUser, @RequestParam(defaultValue = "false") boolean onlyFaulty) {
        return sensorGroupService.getSensorGroups(appUser, onlyFaulty);
    }

    @GetMapping("/groups/{appUserId}")
    @Operation(summary = "appUserId를 기입한 사용자의 모든 센서 조회 ⭐️Admin 전용", description = "<b>appUserId에 해당하는 AppUser가 보유한 SensorGroup의 모든 정보 조회</b><br> <b>🗓️ 장애센서 필터: </b><br> 1. onlyFaulty = true로 보낸다면 장애 센서만 조회<br> 2. onlyFaulty = false혹은 보내지 않는다면 전체 조회 <br> <b>📌 정렬 기준:</b> <br> 1. 센서 그룹 ID 오름차순 <br> 2. 센서 groupPositionNumber 오름차순<br><br> 2025-03-20 update : ***SensorDto.needUpdate***는 생성된 후 3일 이내 update되지 않았거나 최근 update 이력이 3일내로 없는 센서에게 ***true***로 반환")
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
    @Operation(summary = "단일 센서 상세 정보 조회", description = "<b>fault information</b>이 하나라도 <b>true</b>일 경우 <b>status = 오류</b>로 표현 <br> 메모의 경우 전달된<b> appUserId</b>가 작성한 메모 전달")
    public SensorDetailResponseDto getSensorDetail(@PathVariable("sensorId") Long sensorId, @RequestParam Long appUserId) {
        return sensorService.getSensorDetailBySensorId(sensorId, appUserId);
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

    @PatchMapping("/{sensorId}/memo")
    @Operation(summary = "단일 sensor memo 생성 / 수정", description = "<b>단일 sensor 메모</b> <br>***@param*** appUserId와 Token에서 제공한 id가 동일한 경우만 수정 및 메모 가능. 아닐 경우 '권한 외 요청입니다' 반환 <br> 해당 AppUser에게 Sensor 메모가 존재한다면 <b>수정</b> <br> 해당 AppUser에게 Sensor 메모가 존재하지 않는다면<b> 신규 생성</b>")
    public void updateOrCreate(@LoginAppUser AppUser appUser,
                               @PathVariable("sensorId") Long sensorId,
                               @RequestParam Long appUserId,
                               @RequestBody SensorMemoRequestDto sensorMemoRequestDto
    ) {
        sensorService.updateOrCreateSensorMemo(appUser, sensorId, appUserId, sensorMemoRequestDto);
    }

    @PatchMapping("/groups/order")
    @Operation(summary = "전체 sensor group 순서 수정 ⭐️Admin 전용", description = "<b>센서 그룹 순서를 업데이트하는 API</b><br> 프론트엔드에서 새로운 순서로 정렬된 ID 리스트를 받아와 DB의 display_order 값을 갱신<br>만약 의도되지 않은 값, 혹은 실제 DB에 존재하지 않는 sensorGroupId가 포함된다면 해당 sensorGroup을 제외한 나머지 순서만 수정<br>만약 리스트에 존재하지 않고 DB상으로 추가된 sensorGroup이 존재할 경우 가장 후 순위로 지정")
    public void updateSensorGroupOrder(@RequestBody SensorGroupOrderRequestDto requestDto) {
        sensorGroupService.updateSensorGroupOrder(requestDto);
    }

    @PatchMapping("/{sensorId}/relocate")
    @Operation(summary = "단일 sensor 위치정보 update", description = "<b>단일 sensor 위도 경도 수정</b><br> 위도, 경도 수정 요청 시 kakao api 호출하여 address 정보 update 동시 수행")
    public void relocateSensorAddress(@PathVariable("sensorId") Long sensorId, @RequestBody SensorRelocationRequestDto sensorRelocationRequestDto) {
        sensorService.updatedSensorAddress(sensorId, sensorRelocationRequestDto);
    }

    @PostMapping("/log/{sensorGroupId}")
    @Operation(summary = "특정 SensorGroup의 로그 수집", description = "⛔️ 멀티 쓰레드 환경에서 동작하므로 사용에 주의 요함")
    public void fetchSensorLog(@NotNull @PathVariable("sensorGroupId") String sensorGroupId) {
        sensorLogSchedulerService.fetchAndUpdateLogsForSensorGroup(sensorGroupId);
    }

    @DeleteMapping("/log/{sensorGroupId}")
    @Operation(summary = "특정 센서 그룹의 센서 초기화 후 SensorGroup의 로그 수집", description = "⛔️ 멀티 쓰레드 환경에서 동작하므로 사용에 주의 요함")
    public void resetAndFetchSensorLog(@NotNull @PathVariable("sensorGroupId") String sensorGroupId) {
        sensorLogSchedulerService.deleteSensorGroupLogs(sensorGroupId);
        sensorLogSchedulerService.resetAndFetchLogsForSensorGroup(sensorGroupId);
        sensorLogSchedulerService.updateSensorAddress();
    }

    @PutMapping("/log/{sensorGroupId}")
    @Operation(summary = "단일 sensorGroup G명령 전송",
               description = "<b>고정된 command 내용을 가진 외부 API 호출</b><br>센서 그룹 ID 기반 PUT 요청 전송")
    public void sendFixedCommandToSensorGroup(
            @Parameter(example = "0000102140ca63fffe1df1ce")
            @PathVariable String sensorGroupId) {
        thingPlugService.sendFixedGCommand(sensorGroupId);
    }

    @PutMapping("/log/all")
    @Operation(summary = "모든 sensorGroup G명령 전송",
               description = "<b>고정된 command 내용을 가진 외부 API 호출</b><br>센서 그룹 ID 기반 PUT 요청 전송")
    public void sendFixedCommandToSensorGroups() {
        List<String> sensorGroups = thingPlugService.fetchRemoteCSEIds();
        for(String sensorGroupId : sensorGroups) {
            thingPlugService.sendFixedGCommand(sensorGroupId);
        }
    }
}
