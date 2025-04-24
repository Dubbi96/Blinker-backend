package com.blinker.atom.dto.sensor;

import com.blinker.atom.domain.sensor.SensorLog;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
public class SensorLogResponseDto {
    private Long sensorLogId;
    private String cmd;
    private LocalDateTime createdAt;
    private int buttonCount; // 버튼횟수
    private int positionGuideCount; // 위치안내횟수
    private int signalGuideCount; // 신호안내횟수
    private Map<String,Boolean> faultInformation;

    public SensorLogResponseDto(SensorLog sensorLog, String cmd, int buttonCount, int positionGuideCount, int signalGuideCount, Map<String,Boolean> faultInformation) {
        this.sensorLogId = sensorLog.getId();
        this.cmd = cmd;
        this.createdAt = sensorLog.getCreatedAt();
        this.buttonCount = buttonCount;
        this.positionGuideCount = positionGuideCount;
        this.signalGuideCount = signalGuideCount;
        this.faultInformation = faultInformation;
    }

}
