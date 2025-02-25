package com.blinker.atom.dto.sensor;

import com.blinker.atom.domain.sensor.Sensor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDetailResponseDto {
    private String sensorGroupId;
    private String deviceNumber;
    private Long groupPositionNumber;
    private String address;
    private LocalDateTime updatedAt;
    private String status;
    private String memo;

    public SensorDetailResponseDto(Sensor sensor, String status) {
        this.sensorGroupId = sensor.getSensorGroup().getId();
        this.deviceNumber = sensor.getDeviceNumber();
        this.groupPositionNumber = sensor.getGroupPositionNumber();
        this.address = sensor.getAddress();
        this.updatedAt = sensor.getUpdatedAt();
        this.status = status;
        this.memo = sensor.getMemo();
    }
}
