package com.blinker.atom.dto.thingplug;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

@Data
public class SensorUpdateRequestDto {

    @Schema(example = "ea782cd3")
    private String deviceNumber;
    @Schema(example = "1")
    private int deviceId;
    @Schema(example = "75")
    private int positionSignalStrength;
    @Schema(example = "50")
    private int positionSignalThreshold;
    @Schema(example = "80")
    private int communicationSignalStrength;
    @Schema(example = "60")
    private int communicationSignalThreshold;
    @Schema(example = "90")
    private int wireless235Strength;
    @Schema(example = "{\"Proximity\": \"General Proximity\", \"Configuration\": \"Configured\", \"Priority\": \"Female Priority Broadcast\", \"Sound\": \"Cricket\", \"Crossroad\": \"Single Road\", \"Gender\": \"Female\"}")
    private Map<String, String> deviceSettings;
    @Schema(example = "9")
    private Long femaleMute1;
    @Schema(example = "9")
    private Long femaleMute2;
    @Schema(example = "9")
    private Long maleMute1;
    @Schema(example = "9")
    private Long maleMute2;
    @Schema(example = "9")
    private Long birdVolume;
    @Schema(example = "9")
    private Long cricketVolume;
    @Schema(example = "9")
    private Long dingdongVolume;
    @Schema(example = "9")
    private Long femaleVolume;
    @Schema(example = "9")
    private Long minuetVolume;
    @Schema(example = "9")
    private Long maleVolume;
    @Schema(example = "9")
    private Long systemVolume;
    @Schema(example = "30")
    private int communicationInterval;
    @Schema(example = "1")
    private int swVersion;
    @Schema(example = "2")
    private int hwVersion;
    @Schema(example = "cef11dfe")
    private String groupNumber;
    @Schema(example = "4")
    private int signalsInGroup;
    @Schema(example = "3")
    private int groupPositionNumber;
    @Schema(example = "1")
    private int dataType;
    @Schema(example = "100")
    private int sequenceNumber;
}