package com.blinker.atom.service.sensor;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.AppUserSensorGroupRepository;
import com.blinker.atom.domain.sensor.Sensor;
import com.blinker.atom.domain.sensor.SensorLog;
import com.blinker.atom.domain.sensor.SensorLogRepository;
import com.blinker.atom.domain.sensor.SensorRepository;
import com.blinker.atom.dto.sensor.SensorDetailResponseDto;
import com.blinker.atom.dto.sensor.SensorGroupResponseDto;
import com.blinker.atom.dto.sensor.SensorLogResponseDto;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.ParsingUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SensorService {

    private final ObjectMapper objectMapper;
    private final SensorRepository sensorRepository;
    private final SensorLogRepository sensorLogRepository;
    private final AppUserSensorGroupRepository appUserSensorGroupRepository;

    @Transactional(readOnly = true)
    public List<SensorLogResponseDto> getSensorLogsBySensorId(Long sensorId, AppUser appUser, Integer year, Integer month, Integer day) {
        Sensor sensor = sensorRepository.findSensorById(sensorId)
                .orElseThrow(() -> new CustomException(ErrorValue.SENSOR_NOT_FOUND.getMessage()));

        boolean isAppUserAuthorized = appUserSensorGroupRepository.existsByAppUserAndSensorGroup(appUser, sensor.getSensorGroup());
        if (!isAppUserAuthorized) {
            throw new CustomException(ErrorValue.UNAUTHORIZED_SERVICE.getMessage());
        }

        // 날짜 필터 검증
        validateDateFilter(year, month, day);

        // 날짜 범위 설정
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (year != null) {
            LocalDateTime[] dateRange = determineDateRange(year, month, day);
            startDate = dateRange[0];
            endDate = dateRange[1];
        }

        // 로그 조회
        List<SensorLog> sensorLogs = (startDate != null)
            ? sensorLogRepository.getSensorLogsBySensorDeviceNumberAndDateRange(sensor.getDeviceNumber(), startDate, endDate)
            : sensorLogRepository.getSensorLogsBySensorDeviceNumber(sensor.getDeviceNumber());

        return sensorLogs.stream()
                .sorted(Comparator.comparing(SensorLog::getCreatedAt).reversed())
                .map(this::parseSensorLog)
                .toList();
    }

    private void validateDateFilter(Integer year, Integer month, Integer day) {
        if ((year == null && month != null) || (year == null && day != null)) {
            throw new CustomException("날짜 필터를 적용하려면 연도와 월을 입력해야 합니다.");
        }
    }

    private LocalDateTime[] determineDateRange(Integer year, Integer month, Integer day) {
        LocalDateTime startDate;
        LocalDateTime endDate;

        if (month == null && day == null) {
            // 연도별 필터
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = startDate.plusYears(1);
        } else if (day == null) {
            // 월별 필터
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = startDate.plusMonths(1);
        } else {
            // 일별 필터
            startDate = LocalDateTime.of(year, month, day, 0, 0);
            endDate = startDate.plusDays(1);
        }

        return new LocalDateTime[]{startDate, endDate};
    }

    private SensorLogResponseDto parseSensorLog(SensorLog sensorLog) {
        try {
            if (sensorLog.getEventDetails() == null) {
                return new SensorLogResponseDto(sensorLog);
            }
            JsonNode jsonNode = objectMapper.readTree(sensorLog.getEventDetails());
            String eventLog = jsonNode.get("con").asText();
            ParsedSensorLogDto parsedSensorLogDto = ParsingUtil.parseMessage(eventLog);
            return new SensorLogResponseDto(sensorLog, eventLog, parsedSensorLogDto.getCmd(), parsedSensorLogDto.getFaultInformation());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Json Parsing에 실패하였습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public SensorDetailResponseDto getSensorDetailBySensorId(Long sensorId, AppUser appUser) {
        Sensor sensor = sensorRepository.findSensorById(sensorId).orElseThrow(() -> new CustomException(ErrorValue.SENSOR_NOT_FOUND.getMessage()));
        boolean isAppUserAuthorized = appUserSensorGroupRepository.existsByAppUserAndSensorGroup(appUser, sensor.getSensorGroup());
        if(!isAppUserAuthorized) {
            throw new CustomException(ErrorValue.UNAUTHORIZED_SERVICE.getMessage());
        }
        String status = "정상";
        if(sensor.getFaultInformation().containsValue(true)) status = "오류";
        return new SensorDetailResponseDto(sensor, status);
    }
}