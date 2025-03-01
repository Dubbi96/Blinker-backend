package com.blinker.atom.controller;

import com.blinker.atom.service.scheduled.AppUserSensorGroupService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/app-user/{appUserId}/")
@RequiredArgsConstructor
public class AppUserSensorGroupController {

    private final AppUserSensorGroupService appUserSensorGroupService;

    @PostMapping("/{sensorGroupId}")
    @Operation(summary = "특정 유저에게 센서그룹 할당 ⭐️Admin 전용", description = "<b>appUserId에 해당하는 유저에게 특정 sensorGroup 할당</b>")
    public void assignSensorGroup(@PathVariable("appUserId") Long appUserId, @PathVariable("sensorGroupId") String sensorGroupId) {
        appUserSensorGroupService.assignSensorGroupToAppUser(appUserId, sensorGroupId);
    }

    @DeleteMapping("/{sensorGroupId}")
    @Operation(summary = "특정 유저에게 센서그룹 할당 해제 ⭐️Admin 전용", description = "<b>appUserId에 해당하는 유저에게 할당 되었던 특정 sensorGroup 할당 해제</b>")
    public void unassignSensorGroup(@PathVariable("appUserId") Long appUserId, @PathVariable("sensorGroupId") String sensorGroupId) {
        appUserSensorGroupService.unAssignSensorGroupToAppUser(appUserId, sensorGroupId);
    }
}
