package com.blinker.atom.dto.sensor;

import com.blinker.atom.domain.sensor.Sensor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SensorDetailResponseDto {
    private Long sensorId;
    private String deviceNumber;
    private Long groupPositionNumber;
    private Double latitude;
    private Double longitude;
    private LocalDateTime updatedAt;
    private String status;
    private String memo;

    public SensorDetailResponseDto(Sensor sensor, String status) {
        this.sensorId = sensor.getId();
        this.deviceNumber = sensor.getDeviceNumber();
        this.groupPositionNumber = sensor.getGroupPositionNumber();
        this.latitude = sensor.getLatitude();
        this.longitude = sensor.getLongitude();
        this.updatedAt = sensor.getUpdatedAt();
        this.status = status;
        this.memo = sensor.getMemo();
    }
}
