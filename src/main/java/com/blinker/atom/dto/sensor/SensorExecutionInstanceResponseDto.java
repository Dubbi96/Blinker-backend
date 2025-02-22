package com.blinker.atom.dto.sensor;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SensorExecutionInstanceResponseDto {
    private String managementCommandType;
    private String eventCode;
    private LocalDateTime createdAt;
    private String executeStatus;
    private String executeResult;
    private String executeTarget;
    private String executeRequestArgument;
}
