package com.blinker.atom.dto.sensor;

import com.blinker.atom.domain.sensor.SensorLog;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
public class SensorLogResponseDto {
    private Long logId;
    private String cmd;
    private LocalDateTime createdAt;
    private String eventCode;
    private String eventDetails;
    private String sensorGroupId;
    private String sensorDeviceNumber;
    private String eventLog;
    private Map<String,Boolean> faultInformation;

    public SensorLogResponseDto(SensorLog sensorLog, String eventLog, String cmd, Map<String,Boolean> faultInformation) {
        this.logId = sensorLog.getId();
        this.cmd = cmd;
        this.createdAt = sensorLog.getCreatedAt();
        this.eventCode = sensorLog.getEventCode();
        this.eventDetails = sensorLog.getEventDetails();
        this.sensorGroupId = sensorLog.getSensorGroup().getId();
        this.sensorDeviceNumber = sensorLog.getSensorDeviceNumber();
        this.eventLog = eventLog;
        this.faultInformation = faultInformation;
    }

    public SensorLogResponseDto(SensorLog sensorLog) {
        this.logId = sensorLog.getId();
        this.eventCode = sensorLog.getEventCode();
        this.sensorGroupId = sensorLog.getSensorGroup().getId();
        this.sensorDeviceNumber = sensorLog.getSensorDeviceNumber();
    }

}
