package com.blinker.atom.service.sensor;

import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.sensor.SensorGroup;
import com.blinker.atom.domain.sensor.SensorGroupRepository;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SensorGroupService {

    private final SensorGroupRepository sensorGroupRepository;

    /**@LoginAppUser 토큰에서 가져온 AppUser가 보유한 SensorGroup의 모든 정보 조회*/
    public List<SensorGroupResponseDto> getSensorGroups(AppUser appUser) {
        List<SensorGroup> sensorGroups = sensorGroupRepository.findSensorGroupsWithSensorsByUserId(appUser.getId());

        return sensorGroups.stream()
                .map(SensorGroupResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * Create Sensor Group
     */
    private void createSensorGroup() {

    }

    /**
     *
     *  */


}
