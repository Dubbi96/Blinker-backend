package com.blinker.atom.dto.sensor;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class SensorGroupOrderRequestDto {
    @Schema(
            description = "새로운 SensorGroup 순서 ID 목록",
            type = "array",
            example = "[\"0000102140ca63fffe1df1ce\", \"0000102140ca63fffe1df196\", \"00001021702c1ffffe5be3f2\", \"00001021702c1ffffe5be385\", \"00001021702c1ffffe5be3b2\", \"00001021702c1ffffe5be44f\", \"00001021702c1ffffe5be3c5\", \"00001021702c1ffffe5be312\", \"00001021702c1ffffe5be3ac\", \"00001021702c1ffffe5be323\", \"00001021702c1ffffe5be452\", \"00001021702c1ffffe5be300\", \"00001021702c1ffffe5be48d\", \"00001021702c1ffffe5be3c6\", \"00001021702c1ffffe5be3fb\", \"0000102140ca63fffe1df594\", \"0000102140ca63fffe1e298f\", \"0000102140ca63fffe1e257d\", \"0000102140ca63fffe1e231e\", \"0000102140ca63fffe1e299f\"]"
    )
    List<String> sensorGroupIds;
}
