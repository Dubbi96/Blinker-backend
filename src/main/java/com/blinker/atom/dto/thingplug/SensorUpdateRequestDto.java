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
    private int signalStrength;
    @Schema(example = "50")
    private int signalThreshold;
    @Schema(example = "80")
    private int commSignalStrength;
    @Schema(example = "60")
    private int commSignalThreshold;
    @Schema(example = "90")
    private int wireless235Strength;
    @Schema(example = "{\"Proximity\": \"General Proximity\", \"Configuration\": \"Configured\", \"Priority\": \"Female Priority Broadcast\", \"Sound\": \"Cricket\", \"Crossroad\": \"Single Road\", \"Gender\": \"Female\"}")
    private Map<String, String> deviceSettings;
    @Schema(example = "{\"Bird Volume\": 9, \"Cricket Volume\": 9, \"Dingdong Volume\": 9, \"Female Volume\": 9, \"Male Volume\": 9, \"Minuet Volume\": 9, \"System Volume\": 9}")
    private Map<String, Integer> volumeSettings;
    @Schema(example = "{\"Female Mute 1\": 9, \"Female Mute 2\": 9, \"Male Mute 1\": 9, \"Male Mute 2\": 9}")
    private Map<String, Integer> silentSettings;
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