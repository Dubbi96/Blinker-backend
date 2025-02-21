package com.blinker.atom.dto.sensor;

import com.blinker.atom.domain.sensor.SensorGroup;
import lombok.Data;

@Data
public class UnregisteredSensorGroupResponseDto {
    private String sensorGroupId;
    private Double longitude;
    private Double latitude;

    public UnregisteredSensorGroupResponseDto(SensorGroup sensorGroup, Double longitude, Double latitude) {
        this.sensorGroupId = sensorGroup.getId();
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
