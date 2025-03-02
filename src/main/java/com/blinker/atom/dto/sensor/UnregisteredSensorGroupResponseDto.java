package com.blinker.atom.dto.sensor;

import com.blinker.atom.domain.sensor.SensorGroup;
import lombok.Data;

@Data
public class UnregisteredSensorGroupResponseDto {
    private String sensorGroupId;
    private String address;

    public UnregisteredSensorGroupResponseDto(SensorGroup sensorGroup, String address) {
        this.sensorGroupId = sensorGroup.getId();
        this.address = address;
    }
}
