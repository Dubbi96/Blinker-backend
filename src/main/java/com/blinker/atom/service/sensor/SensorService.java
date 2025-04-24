package com.blinker.atom.service.sensor;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.appuser.*;
import com.blinker.atom.domain.sensor.Sensor;
import com.blinker.atom.domain.sensor.SensorLog;
import com.blinker.atom.domain.sensor.SensorLogRepository;
import com.blinker.atom.domain.sensor.SensorRepository;
import com.blinker.atom.dto.sensor.SensorDetailResponseDto;
import com.blinker.atom.dto.sensor.SensorLogResponseDto;
import com.blinker.atom.dto.sensor.SensorMemoRequestDto;
import com.blinker.atom.dto.sensor.SensorRelocationRequestDto;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.GCSUtil;
import com.blinker.atom.util.ParsingUtil;
import com.blinker.atom.util.httpclientutil.HttpClientUtil;
import com.blinker.atom.util.httpclientutil.KakaoHeaderProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SensorService {

    private final ObjectMapper objectMapper;
    private final SensorRepository sensorRepository;
    private final SensorLogRepository sensorLogRepository;
    private final AppUserSensorGroupRepository appUserSensorGroupRepository;
    private final AppUserSensorRepository appUserSensorRepository;
    private final AppUserRepository appUserRepository;
    private final GCSUtil gcsUtil;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=%s&y=%s";

    @Transactional(readOnly = true)
    public List<SensorLogResponseDto> getSensorLogsBySensorId(Long sensorId, AppUser appUser, Integer year, Integer month, Integer day) {
        Sensor sensor = sensorRepository.findSensorById(sensorId)
                .orElseThrow(() -> new CustomException(ErrorValue.SENSOR_NOT_FOUND.getMessage()));

        boolean isAppUserAuthorized = appUserSensorGroupRepository.existsByAppUserAndSensorGroup(appUser, sensor.getSensorGroup());
        if (!isAppUserAuthorized) {
            throw new CustomException(ErrorValue.UNAUTHORIZED_SERVICE.getMessage());
        }

        validateDateFilter(year, month, day);

        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        if (year != null) {
            LocalDateTime[] dateRange = determineDateRange(year, month, day);
            startDate = dateRange[0];
            endDate = dateRange[1];
        }
        if (startDate == null) {
            startDate = LocalDateTime.of(LocalDate.now().getYear(), LocalDate.now().getMonth(), LocalDate.now().getDayOfMonth(), 0, 0);
            endDate = LocalDateTime.now();
        }

        LocalDate today = LocalDate.now();
        List<SensorLogResponseDto> totalLogs = new ArrayList<>();

        // 오늘보다 이전 로그는 GCS에서 조회
        if (startDate.toLocalDate().isBefore(today)) {
            LocalDateTime gcsEndDate = endDate.toLocalDate().isBefore(today)
                    ? endDate
                    : today.minusDays(1).atTime(LocalTime.MAX);
            totalLogs.addAll(loadLogsFromGCS(sensor.getDeviceNumber(), startDate, gcsEndDate));
        }

        // 오늘 날짜 로그는 DB에서 조회
        if (!endDate.toLocalDate().isBefore(today)) {
            LocalDateTime dbStart = startDate.toLocalDate().isBefore(today)
                    ? today.atStartOfDay()
                    : startDate;
            LocalDateTime dbEnd = endDate.isAfter(LocalDateTime.now()) ? LocalDateTime.now() : endDate;

            List<SensorLog> sensorLogs = sensorLogRepository.getSensorLogsBySensorDeviceNumberAndDateRange(
                    sensor.getDeviceNumber(), dbStart, dbEnd
            );
            totalLogs.addAll(sensorLogs.stream()
                    .map(this::parseSensorLog)
                    .toList());
        }

        return totalLogs.stream()
                .sorted(Comparator.comparing(SensorLogResponseDto::getCreatedAt).reversed())
                .toList();
    }

    private List<SensorLogResponseDto> loadLogsFromGCS(String deviceNumber, LocalDateTime startDate, LocalDateTime endDate) {
        List<SensorLogResponseDto> result = new ArrayList<>();
        LocalDate date = startDate.toLocalDate();

        while (!date.isAfter(endDate.toLocalDate())) {
            String filename = String.format("%s_%s.csv", deviceNumber, date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            try {
                InputStream fileStream = gcsUtil.downloadFileFromGCS("sensor-log-archive/" + filename);
                List<SensorLog> parsedLogs = parseCsvToSensorLogs(fileStream);
                result.addAll(parsedLogs.stream().map(this::parseSensorLog).toList());
            } catch (FileNotFoundException e) {
                log.warn("파일 없음: {}", filename);
            } catch (Exception e) {
                log.error("GCS 파일 로딩 실패: {}", filename, e);
            }
            date = date.plusDays(1);
        }

        result.sort(Comparator.comparing(SensorLogResponseDto::getCreatedAt).reversed());
        return result;
    }

    private List<SensorLog> parseCsvToSensorLogs(InputStream inputStream) throws IOException {
        List<SensorLog> logs = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] line;
            boolean isHeader = true;

            while ((line = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                if (line.length < 7) continue;

                String createdAtStr = line[6].trim();
                String eventDetails = line[3].trim();
                String deviceNumber = line[4].trim();

                SensorLog log = new SensorLog();
                log.setCreatedAt(LocalDateTime.parse(createdAtStr, formatter));
                log.setEventDetails(eventDetails);
                log.setSensorDeviceNumber(deviceNumber);

                logs.add(log);
            }
        } catch (CsvValidationException e) {
            throw new IOException("CSV validation error", e);
        }

        return logs;
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
            startDate = LocalDateTime.of(year, 1, 1, 0, 0);
            endDate = LocalDateTime.of(year, 12, 31, 23, 59, 59, 999_999_999);
        } else if (day == null) {
            YearMonth ym = YearMonth.of(year, month);
            LocalDate lastDayOfMonth = ym.atEndOfMonth();
            startDate = LocalDateTime.of(year, month, 1, 0, 0);
            endDate = LocalDateTime.of(lastDayOfMonth, LocalTime.MAX);
        } else {
            LocalDate date = LocalDate.of(year, month, day);
            startDate = date.atStartOfDay();
            endDate = date.atTime(LocalTime.MAX);
        }

        return new LocalDateTime[]{startDate, endDate};
    }

    private SensorLogResponseDto parseSensorLog(SensorLog sensorLog) {
        try {
            JsonNode jsonNode = objectMapper.readTree(sensorLog.getEventDetails());
            String eventLog = jsonNode.get("con").asText();
            ParsedSensorLogDto parsedSensorLogDto = ParsingUtil.parseMessage(eventLog);
            return new SensorLogResponseDto(sensorLog, parsedSensorLogDto.getCmd(), parsedSensorLogDto.getButtonCount(), parsedSensorLogDto.getPositionGuideCount(), parsedSensorLogDto.getSignalGuideCount(), parsedSensorLogDto.getFaultInformation());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Json Parsing에 실패하였습니다.", e);
        }
    }

    @Transactional(readOnly = true)
    public SensorDetailResponseDto getSensorDetailBySensorId(Long sensorId, Long appUserId) {
        AppUser appUser = appUserRepository.findAppUserById(appUserId).orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        Sensor sensor = sensorRepository.findSensorById(sensorId).orElseThrow(() -> new CustomException(ErrorValue.SENSOR_NOT_FOUND.getMessage()));
        boolean isAppUserAuthorized = appUserSensorGroupRepository.existsByAppUserAndSensorGroup(appUser, sensor.getSensorGroup());
        AppUserSensor appUserSensor = appUserSensorRepository.findBySensorAndAppUser(sensor, appUser).orElse(null);
        if(!isAppUserAuthorized) {
            throw new CustomException(ErrorValue.UNAUTHORIZED_SERVICE.getMessage());
        }
        String status = "정상";
        if(sensor.getFaultInformation().containsValue(true)) status = "오류";

        return new SensorDetailResponseDto(sensor, status, appUserSensor == null ? null : appUserSensor.getMemo());
    }

    @Transactional
    public void updateOrCreateSensorMemo(AppUser appUser, Long sensorId, Long appUserId, SensorMemoRequestDto sensorMemoRequestDto) {
        if (!appUser.getId().equals(appUserId)) throw new CustomException(ErrorValue.UNAUTHORIZED_SERVICE.getMessage());
        Sensor sensor = sensorRepository.findSensorById(sensorId).orElseThrow(() -> new CustomException(ErrorValue.SENSOR_NOT_FOUND.getMessage()));
        AppUserSensor appUserSensor = appUserSensorRepository.findBySensorAndAppUser(sensor, appUser)
                .orElse(AppUserSensor.builder()
                        .appUser(appUser)
                        .sensor(sensor)
                        .build());
        appUserSensor.updateMemo(sensorMemoRequestDto.getMemo());
        appUserSensorRepository.save(appUserSensor);
    }

    @Transactional
    public void updatedSensorAddress(Long sensorId, SensorRelocationRequestDto sensorRelocationRequestDto){
        Sensor sensor = sensorRepository.findSensorById(sensorId).orElseThrow(() -> new CustomException(ErrorValue.SENSOR_NOT_FOUND.getMessage()));
        String url = String.format(KAKAO_API_URL, sensorRelocationRequestDto.getLongitude(), sensorRelocationRequestDto.getLatitude());

        // Kakao HeaderProvider 사용
        String response = HttpClientUtil.get(url, new KakaoHeaderProvider("KakaoAK "+kakaoRestApiKey));

        if (response == null || response.isEmpty()) {
            return;
        }
        // API 응답에서 주소 추출
        sensor.updateLocation(sensorRelocationRequestDto.getLatitude(), sensorRelocationRequestDto.getLongitude());
        String address = extractAddressFromResponse(response);
        sensor.updateAddress(address);
        sensorRepository.save(sensor);
    }


    /** Kakao 응답에서 첫 번째 주소지 파싱 */
    private String extractAddressFromResponse(String response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode documentsNode = rootNode.get("documents");
            if (documentsNode == null || !documentsNode.isArray() || documentsNode.isEmpty()) {
                return "주소 정보 없음";
            }
            JsonNode firstDocument = documentsNode.get(0);
            JsonNode addressNode = firstDocument.get("address");
            if (addressNode == null || addressNode.get("address_name") == null) {
                return "주소 정보 없음";
            }
            return addressNode.get("address_name").asText();
        } catch (Exception e) {
            return "주소 정보 없음";
        }
    }
}