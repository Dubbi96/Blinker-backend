package com.blinker.atom.service.sensor;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.AppUserRepository;
import com.blinker.atom.domain.sensor.Sensor;
import com.blinker.atom.domain.sensor.SensorGroup;
import com.blinker.atom.domain.sensor.SensorGroupRepository;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorGroupService {

    private final AppUserRepository appUserRepository;
    private final SensorGroupRepository sensorGroupRepository;

    /**@LoginAppUser 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회*/
    @Transactional(readOnly = true)
    public List<SensorGroupResponseDto> getSensorGroups(AppUser appUser) {
        return getSensorGroupWithSensorsByUserId(appUser);
    }

    /**ID를 기반으로 AppUser를 조회, AppUser가 보유한 SensorGroup의 모든 정보 조회*/
    @Transactional(readOnly = true)
    public List<SensorGroupResponseDto> getSensorGroupsByAppUserId(Long appUserId) {
        AppUser appUser = appUserRepository.findById(appUserId).orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        return getSensorGroupWithSensorsByUserId(appUser);
    }

    @NotNull
    private List<SensorGroupResponseDto> getSensorGroupWithSensorsByUserId(AppUser appUser) {
        List<SensorGroup> sensorGroups = sensorGroupRepository.findSensorGroupsWithSensorsByUserId(appUser.getId());

        return sensorGroups.stream()
                .sorted(Comparator.comparing(SensorGroup::getId)) //SensorGroup의 id 순서로 정렬
                .map(sensorGroup -> {
                    // SensorDto 내부에서 groupPositionNumber 기준으로 정렬
                    SensorGroupResponseDto dto = new SensorGroupResponseDto(sensorGroup);
                    dto.setSensors(dto.getSensors().stream()
                            .sorted(Comparator.comparing(SensorGroupResponseDto.SensorDto::getGroupPositionNumber))
                            .toList());
                    return dto;})
                .toList();
    }
}
