package com.blinker.atom.service.sensor;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.AppUserRepository;
import com.blinker.atom.domain.sensor.*;
import com.blinker.atom.dto.sensor.SensorGroupOrderRequestDto;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import com.blinker.atom.dto.sensor.UnregisteredSensorGroupResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
                .sorted(Comparator.comparing(SensorGroup::getDisplayOrder, Comparator.nullsLast(Long::compareTo)))
                .map(sensorGroup -> {
                    SensorGroupResponseDto dto = new SensorGroupResponseDto(sensorGroup);

                    List<SensorGroupResponseDto.SensorDto> sortedSensors = dto.getSensors().stream()
                            .filter(sensor -> !onlyFaulty || sensor.getFaultInformation().containsValue(true))
                            .sorted(Comparator.comparing(SensorGroupResponseDto.SensorDto::getGroupPositionNumber, Comparator.nullsLast(Long::compareTo)))
                            .toList();

                    dto.setSensors(sortedSensors);

                    if (onlyFaulty) {
                        // 고장 필터링 상태에서, 센서가 없으면 제외하고,
                        // 센서가 있어도 고장 센서가 없으면 제외
                        return sortedSensors.isEmpty() ? null : dto;
                    } else {
                        // 전체 보기 모드 → 무조건 포함
                        return dto;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UnregisteredSensorGroupResponseDto> getUnregisteredSensorGroups() {
        List<SensorGroup> sensorGroups = sensorGroupRepository.findUnrelatedSensorGroups();

        return sensorGroups.stream()
                .sorted(Comparator.comparing(
                    SensorGroup::getDisplayOrder,
                    Comparator.nullsLast(Long::compareTo) // ← Long 타입이라고 가정
                ))
                .map(sensorGroup -> {
                    Sensor s = sensorRepository.findMasterSensorBySensorGroup(sensorGroup.getId())
                            .orElseGet(Sensor::new);
                    return new UnregisteredSensorGroupResponseDto(sensorGroup, s.getAddress());
                })
                .toList();
    }

    @Transactional
    public void updateSensorGroupOrder(SensorGroupOrderRequestDto requestDto) {
        List<String> requestedIds = requestDto.getSensorGroupIds();
        List<SensorGroup> existingGroups = sensorGroupRepository.findAll();

        // 2. 현재 존재하는 ID 목록 추출 (Set을 사용하여 빠르게 체크 가능)
        Set<String> existingIds = existingGroups.stream()
                .map(SensorGroup::getId)
                .collect(Collectors.toSet());

        // 3. 유효한 ID만 필터링 (삭제된 ID 제거)
        List<String> validIds = requestedIds.stream()
                .filter(existingIds::contains)
                .collect(Collectors.toList());

        // 4. 요청되지 않은 신규 SensorGroup 찾기
        List<SensorGroup> newGroups = existingGroups.stream()
                .filter(group -> !validIds.contains(group.getId()))
                .toList();

        // 5. 새로 추가된 ID들을 validIds 뒤에 배치
        validIds.addAll(newGroups.stream().map(SensorGroup::getId).toList());

        // 6. ID → SensorGroup 매핑
        Map<String, SensorGroup> sensorGroupMap = existingGroups.stream()
                .collect(Collectors.toMap(SensorGroup::getId, Function.identity()));

        // 7. 변경이 필요한 SensorGroup만 업데이트
        List<SensorGroup> updatedSensorGroups = new ArrayList<>();
        for (int i = 0; i < validIds.size(); i++) {
            String sensorGroupId = validIds.get(i);
            SensorGroup sensorGroup = sensorGroupMap.get(sensorGroupId);

            if (sensorGroup != null && !sensorGroup.getDisplayOrder().equals((long) i)) {
                sensorGroup.updateOrder((long) i);
                updatedSensorGroups.add(sensorGroup);
            }
        }

        if (!updatedSensorGroups.isEmpty()) {
            sensorGroupRepository.saveAll(updatedSensorGroups);
        }
    }
}
