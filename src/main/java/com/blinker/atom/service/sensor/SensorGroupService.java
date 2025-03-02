package com.blinker.atom.service.sensor;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.AppUserRepository;
import com.blinker.atom.domain.sensor.*;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import com.blinker.atom.dto.sensor.UnregisteredSensorGroupResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorGroupService {

    private final AppUserRepository appUserRepository;
    private final SensorGroupRepository sensorGroupRepository;
    private final SensorRepository sensorRepository;

    /**@LoginAppUser 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회*/
    @Transactional(readOnly = true)
    public List<SensorGroupResponseDto> getSensorGroups(AppUser appUser, boolean onlyFaulty) {
        return getSensorGroupWithSensorsByUserId(appUser, onlyFaulty);
    }

    /**ID를 기반으로 AppUser를 조회, AppUser가 보유한 SensorGroup의 모든 정보 조회*/
    @Transactional(readOnly = true)
    public List<SensorGroupResponseDto> getSensorGroupsByAppUserId(Long appUserId, boolean onlyFaulty) {
        AppUser appUser = appUserRepository.findById(appUserId).orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        return getSensorGroupWithSensorsByUserId(appUser, onlyFaulty);
    }

    @NotNull
    private List<SensorGroupResponseDto> getSensorGroupWithSensorsByUserId(AppUser appUser, boolean onlyFaulty) {
        List<SensorGroup> sensorGroups = sensorGroupRepository.findSensorGroupsWithSensorsByUserId(appUser.getId());

        return sensorGroups.stream()
                .sorted(Comparator.comparing(SensorGroup::getOrder))
                .map(sensorGroup -> {
                    SensorGroupResponseDto dto = new SensorGroupResponseDto(sensorGroup);

                    List<SensorGroupResponseDto.SensorDto> sortedSensors = dto.getSensors().stream()
                        .filter(sensor -> !onlyFaulty || sensor.getFaultInformation().containsValue(true))
                        .sorted(Comparator.comparing(SensorGroupResponseDto.SensorDto::getGroupPositionNumber))
                        .toList();

                    dto.setSensors(sortedSensors);
                    return sortedSensors.isEmpty() && onlyFaulty ? null : dto;
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UnregisteredSensorGroupResponseDto> getUnregisteredSensorGroups() {
        List<SensorGroup> sensorGroups = sensorGroupRepository.findUnrelatedSensorGroups();
        log.info("📌 조회된 센서 그룹 개수: {}", sensorGroups.size());
        return sensorGroups.stream()
                .sorted(Comparator.comparing(SensorGroup::getOrder))
                .map(sensorGroup -> {
                    Sensor s = sensorRepository.findMasterSensorBySensorGroup(sensorGroup.getId()).orElseGet(Sensor::new);
                    return new UnregisteredSensorGroupResponseDto(sensorGroup, s.getAddress());
                })
                .toList();
    }
}
