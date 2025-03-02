package com.blinker.atom.dto.thingplug;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;


@Data
@NotNull
public class SensorUpdateRequestDto {
    @NotNull
    @Schema(example = "ea782cd3")
    private String deviceNumber;
    @NotNull
    @Schema(example = "1")
    private int deviceId;
    @NotNull
    @Schema(example = "75")
    private int positionSignalStrength;
    @NotNull
    @Schema(example = "50")
    private int positionSignalThreshold;
    @NotNull
    @Schema(example = "80")
    private int communicationSignalStrength;
    @NotNull
    @Schema(example = "60")
    private int communicationSignalThreshold;
    @NotNull
    @Schema(example = "90")
    private int wireless235Strength;
    @NotNull
    @Schema(example = "{\"Proximity\": \"General Proximity\", \"Configuration\": \"Configured\", \"Priority\": \"Female Priority Broadcast\", \"Sound\": \"Cricket\", \"Crossroad\": \"Single Road\", \"Gender\": \"Female\"}")
    private Map<String, String> deviceSettings;
    @NotNull
    @Schema(example = "9")
    private Long femaleMute1;
    @NotNull
    @Schema(example = "9")
    private Long femaleMute2;
    @NotNull
    @Schema(example = "9")
    private Long maleMute1;
    @NotNull
    @Schema(example = "9")
    private Long maleMute2;
    @NotNull
    @Schema(example = "9")
    private Long birdVolume;
    @NotNull
    @Schema(example = "9")
    private Long cricketVolume;
    @NotNull
    @Schema(example = "9")
    private Long dingdongVolume;
    @NotNull
    @Schema(example = "9")
    private Long femaleVolume;
    @NotNull
    @Schema(example = "9")
    private Long minuetVolume;
    @NotNull
    @Schema(example = "9")
    private Long maleVolume;
    @NotNull
    @Schema(example = "9")
    private Long systemVolume;
    @NotNull
    @Schema(example = "30")
    private int communicationInterval;
    @NotNull
    @Schema(example = "1")
    private int swVersion;
    @NotNull
    @Schema(example = "2")
    private int hwVersion;
    @NotNull
    @Schema(example = "cef11dfe")
    private String groupKey;
    @NotNull
    @Schema(example = "4")
    private int sensorCount;
    @NotNull
    @Schema(example = "3")
    private int groupPositionNumber;
}