package com.blinker.atom.service.scheduled;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.domain.sensor.*;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.GCSUtil;
import com.blinker.atom.util.ParsingUtil;
import com.blinker.atom.util.XmlUtil;
import com.blinker.atom.util.httpclientutil.HttpClientUtil;
import com.blinker.atom.util.httpclientutil.KakaoHeaderProvider;
import com.blinker.atom.util.httpclientutil.ThingPlugHeaderProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorLogSchedulerService {

    private final SensorGroupRepository sensorGroupRepository;
    private final SensorLogRepository sensorLogRepository;
    private final SensorRepository sensorRepository;
    private final ObjectMapper objectMapper;
    private final GCSUtil gcsUtil;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${thingplug.base.url}")
    private String baseUrl;

    @Value("${thingplug.app.eui}")
    private String appEui;

    @Value("${thingplug.headers.x-m2m-origin}")
    private String origin;

    @Value("${thingplug.headers.uKey}")
    private String uKey;

    @Value("${thingplug.headers.x-m2m-ri}")
    private String requestId;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json?x=%s&y=%s";
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Transactional
    public void rollbackSensors(){
        List<Sensor> sensors = sensorRepository.findAll();
        scheduleRollbackSensors(sensors);
    }

    @Transactional
    protected void scheduleRollbackSensors(List<Sensor> sensors) {
        List<CompletableFuture<Void>> futures = sensors.stream()
            .map(sensor -> CompletableFuture.runAsync(() -> rollbackSensors(sensor), executorService))
            .toList();
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Transactional
    protected void rollbackSensors(Sensor sensor) {
        ParsedSensorLogDto parsedSensorLog = ParsingUtil.parseMessage(sensor.getLastlyModifiedWith());
        Sensor updatedSensor = Sensor.builder()
                .id(sensor.getId())
                .sensorGroup(sensor.getSensorGroup())
                .deviceNumber(parsedSensorLog.getDeviceNumber())
                .deviceId((double) parsedSensorLog.getDeviceId())
                .positionSignalStrength((long) parsedSensorLog.getPositionSignalStrength())
                .positionSignalThreshold((long) parsedSensorLog.getPositionSignalThreshold())
                .communicationSignalStrength((long) parsedSensorLog.getCommSignalStrength())
                .communicationSignalThreshold((long) parsedSensorLog.getCommSignalThreshold())
                .wireless235Strength((long) parsedSensorLog.getWireless235Strength())
                .deviceSetting(List.of(parsedSensorLog.getDeviceSettings().split(", ")))
                .communicationInterval((long) parsedSensorLog.getCommInterval())
                .faultInformation(parsedSensorLog.getFaultInformation())
                .swVersion((long) parsedSensorLog.getSwVersion())
                .hwVersion((long) parsedSensorLog.getHwVersion())
                .buttonCount((long) parsedSensorLog.getButtonCount())
                .positionGuideCount((long) parsedSensorLog.getPositionGuideCount())
                .signalGuideCount((long) parsedSensorLog.getSignalGuideCount())
                .groupPositionNumber((long) parsedSensorLog.getGroupPositionNumber())
                .femaleMute1((long) parsedSensorLog.getSilentSettings().get("Female Mute 1"))
                .femaleMute2((long) parsedSensorLog.getSilentSettings().get("Female Mute 2"))
                .maleMute1((long) parsedSensorLog.getSilentSettings().get("Male Mute 1"))
                .maleMute2((long) parsedSensorLog.getSilentSettings().get("Male Mute 2"))
                .birdVolume((long) parsedSensorLog.getVolumeSettings().get("Bird Volume"))
                .cricketVolume((long) parsedSensorLog.getVolumeSettings().get("Cricket Volume"))
                .dingdongVolume((long) parsedSensorLog.getVolumeSettings().get("Dingdong Volume"))
                .femaleVolume((long) parsedSensorLog.getVolumeSettings().get("Female Volume"))
                .maleVolume((long) parsedSensorLog.getVolumeSettings().get("Male Volume"))
                .minuetVolume((long) parsedSensorLog.getVolumeSettings().get("Minuet Volume"))
                .systemVolume((long) parsedSensorLog.getVolumeSettings().get("System Volume"))
                .latitude(sensor.getLatitude())
                .longitude(sensor.getLongitude())
                .lastlyModifiedWith(sensor.getLastlyModifiedWith())
                .serverTime(decodeServerTime(parsedSensorLog.getServerTime()))
                .updatedAt(sensor.getUpdatedAt())
                .address(sensor.getAddress())
                .build();
        sensorRepository.save(updatedSensor);
    }

    /**
     * 	1.	sensor_group 테이블의 모든 행을 조회.
     * 	2.	HttpClientUtil.get(url, origin, uKey, requestId)를 이용하여 contentInstance 리스트 조회.
     * 	3.	contentInstance의 CI**** 부분을 추출하여 SensorLog의 eventCode로 저장.
     * 	4.	eventCode가 이미 존재하면 저장하지 않음 (ON CONFLICT DO NOTHING).
     * 	5.  eventDetail을 기준으로 sensor 데이터 업데이트
     * 	5-1. eventDetail의 cmd(67일 경우, 77일 경우 등, 분기마다 동작 확인)
     * 	5-1-1. cmd 67일 경우 신호기 번호, 그룹 내 번호 확인 (sensor table id 확인)
     * 	5-1-2. cmd 73일 경우 신호기 수정에 대한 결과 이므로 67과 동일한 절차
     * 	5-1-3. cmd 77일 경우 SSID 요청 응답으로 Sensor Group의 SSID update
     * 	5-1-4. cmd 61일 경우 SSID 수정 결과 응답으로 Sensor Group의 SSID update
     *  5-1-5. cmd 65일 경우 오류 로그임 : 아직 테스트 안되므로 65일 경우 로그 무시
     *  5-1-6. cmd 72일 경우 기기의 상태 정기보고로 들어온 로그, 72번 로그 무시
     *  5-1-7. cmd 74일 경우 기기의 최초 상태를 전달, 74 로그도 무시
     *  5-1-8. cmd 70일 경우 GPS 좌표 장치에 저장한 건, 70번 로그 무시
     *  5-1-9. cmd 71번의 경우도 GPS 좌표이나, 67번, 73로그 둘다 가지고 있으므로 로그 무시
     * 	6.	해당 작업을 Spring Scheduler + Async를 이용해 주기적으로 실행.
     * 	*/
    @Transactional(readOnly = true)
    public void fetchAndSaveSensorLogs() {
        log.info("🔹 Sensor Log 스케줄러 실행...");

        LocalDateTime lastSavedLogTime = sensorLogRepository.findMaxCreatedAt();
        if (lastSavedLogTime == null) {
            lastSavedLogTime = LocalDateTime.now().minusHours(12);
            log.warn("sensor_log 테이블이 비어있습니다. 현재 시각을 기준으로 설정합니다: {}", lastSavedLogTime);
        } else {
            log.info("마지막 저장된 로그 시각: {}", lastSavedLogTime);
        }

        Set<String> existingEventCodes = new HashSet<>(sensorLogRepository.findAllEventCodes());

        // 모든 sensor_group 조회
        List<SensorGroup> sensorGroups = sensorGroupRepository.findAll();

        for (SensorGroup group : sensorGroups) {
            String sensorGroupId = group.getId();
            String url = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa?fu=1&ty=4",
                    baseUrl, appEui, sensorGroupId);

            String response = HttpClientUtil.get(url, new ThingPlugHeaderProvider(origin, uKey, requestId));
            log.info("Fetching Sensor Log at URL: {}", response);
            if (response == null || response.isEmpty()) {
                log.warn("API 응답이 없음. SensorGroup: {}", sensorGroupId);
                continue;
            }
            // XML 파싱하여 contentInstance 리스트 추출
            List<String> eventCodes = extractContentInstanceUri(response);
            List<String> newEventCodes = new ArrayList<>();
            for (String eventCode : eventCodes) {
                if (existingEventCodes.contains(eventCode)) {
                    log.info("중복된 이벤트 코드 (저장되지 않음): {}", eventCode);  // 중복된 이벤트 코드 저장
                } else {
                    newEventCodes.add(eventCode);  // 새로운 이벤트 코드 저장
                }
            }
            // SensorLog 저장 (중복 방지)
            saveSensorLogs(newEventCodes, group, lastSavedLogTime);
        }
    }

    private List<String> extractContentInstanceUri(String response) {
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("/contentInstance-([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
            if (result.isEmpty()) {
                log.warn("No valid Content Instance URI found in the response.");
            }
        return result;
    }

    protected void saveSensorLogs(List<String> eventCodes, SensorGroup group, LocalDateTime lastSavedTime) {
        List<CompletableFuture<Void>> futures = eventCodes.stream()
            .map(eventCode -> CompletableFuture.runAsync(
                () -> {
                    try {
                        fetchAndSaveLog(eventCode, group, lastSavedTime);
                    } catch (Exception e) {
                        log.error("❌ fetchAndSaveLog 처리 실패: eventCode = {}", eventCode, e);
                    }
                }, executorService))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void fetchAndSaveLog(String eventCode, SensorGroup group, LocalDateTime lastSavedTime) {
        String contentInstanceUrl = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa/contentInstance-%s",
                baseUrl, appEui, group.getId(), eventCode);
        try {
            String contentInstanceResponse = HttpClientUtil.get(contentInstanceUrl, new ThingPlugHeaderProvider(origin, uKey, requestId));
            String jsonEventDetail = XmlUtil.convertXmlToJson(contentInstanceResponse);
            SensorGroup existingGroup = sensorGroupRepository.findById(group.getId()).orElse(null);
            if (existingGroup == null) {
                log.warn("⚠SensorGroup {}가 존재하지 않음. 새로운 그룹을 삽입하지 않음.", group.getId());
                return;
            }

            JsonNode jsonNode = objectMapper.readTree(jsonEventDetail);
            if (!jsonNode.has("con")) {
                log.error("❌ JSON 응답에 'con' 필드가 없음. eventCode: {}", eventCode);
                return;
            }

            LocalDateTime eventDateTime = OffsetDateTime.parse(jsonNode.get("ct").asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();
            if (lastSavedTime != null && !eventDateTime.isAfter(lastSavedTime)) {
                log.info("🕒 이미 저장된 시간 이전 로그: {}, 저장 생략", eventCode);
                return;
            }

            // 🌟 'con' 문자열에서 2~10번째 문자 추출하여 sensorDeviceNumber에 저장
            String conValue = jsonNode.get("con").asText();
            String sensorDeviceNumber = (conValue.length() >= 10) ? conValue.substring(2, 10) : "";

            SensorLog logEntry = SensorLog.builder()
                    .createdAt(eventDateTime)
                    .sensorGroup(existingGroup)
                    .eventCode(eventCode)
                    .eventDetails(jsonEventDetail)
                    .sensorDeviceNumber(sensorDeviceNumber)
                    .isProcessed(false)
                    .build();

            sensorLogRepository.save(logEntry);
        } catch (HttpClientErrorException e) {
            log.error("HTTP Error during Content Instance processing. Status: {}, Error: {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException("400 Bad Request: Check remoteCSE ID or request parameters.");
            }
            throw e;
        } catch (JsonProcessingException e) {
            log.error("Unexpected error during Content Instance processing", e);
            throw new CustomException("Unexpected error during Content Instance processing");
        }
    }

    /**
     * 전체 프로세스를 비동기로 실행 (멀티스레드 없이)
     */
    @Transactional
    public void updateSensorFromSensorLogs() {
        log.info("Sensor 업데이트 시작...");

        // 1️⃣ 아직 처리되지 않은 SensorLog 조회
        List<SensorLog> sensorLogs = sensorLogRepository.findUnprocessedLogs(LocalDateTime.now().minusHours(24));
        log.info("총 {}개의 SensorLog 처리 예정", sensorLogs.size());

        for (SensorLog logEntry : sensorLogs) {
            processSensorLog(logEntry);
        }

        log.info("✅ Sensor Log 업데이트 완료");
    }

    /**
     * **로그 하나를 처리하는 메서드 (동기 실행)**
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSensorLog(SensorLog logEntry) {
        try {
            SensorGroup group = sensorGroupRepository.findById(logEntry.getSensorGroup().getId()).orElse(null);
            if (group == null) {
                log.warn("⚠ SensorGroup {}을 찾을 수 없음. 로그 처리 스킵", logEntry.getSensorGroup().getId());
                markAsProcessed(logEntry);
                return;
            }

            JsonNode jsonNode = objectMapper.readTree(logEntry.getEventDetails());
            String eventCode = logEntry.getEventCode();

            if (!jsonNode.has("con")) {
                log.error("❌ JSON 응답에 'con' 필드가 없음. eventCode: {}", eventCode);
                markAsProcessed(logEntry);
                return;
            }
            String sensorLogContentInstance = jsonNode.get("con").asText();
            ParsedSensorLogDto parsedSensorLog = ParsingUtil.parseMessage(sensorLogContentInstance);

            if (!jsonNode.has("ct")) {
                log.error("❌ JSON 응답에 'ct' 필드가 없음. eventCode: {}", eventCode);
                markAsProcessed(logEntry);
                return;
            }
            String sensorLogCreatedTimeAsString = jsonNode.get("ct").asText();
            LocalDateTime sensorLogCreatedTime = OffsetDateTime.parse(sensorLogCreatedTimeAsString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();

            if (parsedSensorLog.getCmd().equals("67") || parsedSensorLog.getCmd().equals("73")) {
                List<String> sensorLogLatitudeAndLongitudeAsString = new ArrayList<>();
                if (jsonNode.has("ppt") && jsonNode.get("ppt").has("gwl")) {
                    String gwlText = jsonNode.get("ppt").get("gwl").asText();
                    if (!gwlText.isEmpty()) {
                        sensorLogLatitudeAndLongitudeAsString = List.of(gwlText.split(","));
                    }
                }

                Optional<Sensor> optionalSensor = sensorRepository.findSensorByDeviceNumber(parsedSensorLog.getDeviceNumber());
                if (optionalSensor.isPresent()) {
                    Sensor existingSensor = optionalSensor.get();
                    if (existingSensor.getUpdatedAt() != null &&
                       (existingSensor.getUpdatedAt().isAfter(sensorLogCreatedTime) ||
                        existingSensor.getUpdatedAt().isEqual(sensorLogCreatedTime))) {
                        markAsProcessed(logEntry);
                        return;
                    }
                    updateSensor(existingSensor, parsedSensorLog, sensorLogLatitudeAndLongitudeAsString, sensorLogContentInstance);
                    sensorRepository.save(existingSensor);
                    log.info("🆙 센서정보 업데이트 됨 : {} ", existingSensor.getDeviceNumber());
                } else {
                    Sensor newSensor = createNewSensor(parsedSensorLog, logEntry.getSensorGroup(), sensorLogLatitudeAndLongitudeAsString, sensorLogContentInstance);
                    log.info("🆕 새로운 센서 저장 됨 : {} ", newSensor.getDeviceNumber());
                }

                if (group.getUpdatedAt() != null &&
                   (group.getUpdatedAt().isAfter(sensorLogCreatedTime) ||
                    group.getUpdatedAt().isEqual(sensorLogCreatedTime))) {
                    markAsProcessed(logEntry);
                    return;
                }
                sensorGroupRepository.updateSensorGroup(group.getId(), parsedSensorLog.getGroupNumber(), parsedSensorLog.getSignalsInGroup());
            }

            if (parsedSensorLog.getCmd().equals("61") || parsedSensorLog.getCmd().equals("77")) {
                if (group.getSsidUpdatedAt() != null &&
                   (group.getSsidUpdatedAt().isAfter(sensorLogCreatedTime) ||
                    group.getSsidUpdatedAt().isEqual(sensorLogCreatedTime))) {
                    markAsProcessed(logEntry);
                    return;
                }
                sensorGroupRepository.updateSsid(group.getId(), trimAfterFirstZero(sensorLogContentInstance.substring(12, 76)));
            }

            markAsProcessed(logEntry);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Json Parsing에 실패하였습니다.",e);
        }
    }

    private String trimAfterFirstZero(String input) {
        int firstZeroIndex = input.indexOf("00");
        if (firstZeroIndex == -1) {
            return input;
        }
        return input.substring(0, firstZeroIndex);
    }

    @Transactional
    protected void markAsProcessed(SensorLog logEntry) {
        logEntry.markAsProcessed();
        sensorLogRepository.save(logEntry);
        log.info("SensorLog {} 를 isProcessed = true 로 설정", logEntry.getEventCode());
    }

    @Transactional
    protected Sensor createNewSensor(ParsedSensorLogDto parsedSensorLog, SensorGroup sensorGroup, List<String> sensorLogLatitudeAndLongitudeAsString, String sensorLogContentInstance) {
        // Sensor 생성 및 저장
        Sensor sensor = Sensor.builder()
                .sensorGroup(sensorGroup)
                .deviceNumber(parsedSensorLog.getDeviceNumber())
                .deviceId((double) parsedSensorLog.getDeviceId())
                .positionSignalStrength((long) parsedSensorLog.getPositionSignalStrength())
                .positionSignalThreshold((long) parsedSensorLog.getPositionSignalThreshold())
                .communicationSignalStrength((long) parsedSensorLog.getCommSignalStrength())
                .communicationSignalThreshold((long) parsedSensorLog.getCommSignalThreshold())
                .wireless235Strength((long) parsedSensorLog.getWireless235Strength())
                .deviceSetting(List.of(parsedSensorLog.getDeviceSettings().split(", ")))
                .communicationInterval((long) parsedSensorLog.getCommInterval())
                .faultInformation(parsedSensorLog.getFaultInformation())
                .swVersion((long) parsedSensorLog.getSwVersion())
                .hwVersion((long) parsedSensorLog.getHwVersion())
                .buttonCount((long) parsedSensorLog.getButtonCount())
                .positionGuideCount((long) parsedSensorLog.getPositionGuideCount())
                .signalGuideCount((long) parsedSensorLog.getSignalGuideCount())
                .groupPositionNumber((long) parsedSensorLog.getGroupPositionNumber())
                .femaleMute1((long) parsedSensorLog.getSilentSettings().get("Female Mute 1"))
                .femaleMute2((long) parsedSensorLog.getSilentSettings().get("Female Mute 2"))
                .maleMute1((long) parsedSensorLog.getSilentSettings().get("Male Mute 1"))
                .maleMute2((long) parsedSensorLog.getSilentSettings().get("Male Mute 2"))
                .birdVolume((long) parsedSensorLog.getVolumeSettings().get("Bird Volume"))
                .cricketVolume((long) parsedSensorLog.getVolumeSettings().get("Cricket Volume"))
                .dingdongVolume((long) parsedSensorLog.getVolumeSettings().get("Dingdong Volume"))
                .femaleVolume((long) parsedSensorLog.getVolumeSettings().get("Female Volume"))
                .maleVolume((long) parsedSensorLog.getVolumeSettings().get("Male Volume"))
                .minuetVolume((long) parsedSensorLog.getVolumeSettings().get("Minuet Volume"))
                .systemVolume((long) parsedSensorLog.getVolumeSettings().get("System Volume"))
                .latitude(Double.parseDouble(!sensorLogLatitudeAndLongitudeAsString.isEmpty() ? sensorLogLatitudeAndLongitudeAsString.get(0) : "0"))
                .longitude(Double.parseDouble(!sensorLogLatitudeAndLongitudeAsString.isEmpty() ? sensorLogLatitudeAndLongitudeAsString.get(1) : "0"))
                .lastlyModifiedWith(sensorLogContentInstance)
                .build();

        return sensorRepository.saveAndFlush(sensor);
    }

    @Transactional
    protected void updateSensor(Sensor sensor, ParsedSensorLogDto parsedSensorLog, List<String> sensorLogLatitudeAndLongitudeAsString, String sensorLogContentInstance) {
        // Sensor 생성
        Sensor updatedSensor = Sensor.builder()
                .id(sensor.getId())
                .sensorGroup(sensor.getSensorGroup())
                .deviceNumber(parsedSensorLog.getDeviceNumber())
                .deviceId((double) parsedSensorLog.getDeviceId())
                .positionSignalStrength((long) parsedSensorLog.getPositionSignalStrength())
                .positionSignalThreshold((long) parsedSensorLog.getPositionSignalThreshold())
                .communicationSignalStrength((long) parsedSensorLog.getCommSignalStrength())
                .communicationSignalThreshold((long) parsedSensorLog.getCommSignalThreshold())
                .wireless235Strength((long) parsedSensorLog.getWireless235Strength())
                .deviceSetting(List.of(parsedSensorLog.getDeviceSettings().split(", ")))
                .communicationInterval((long) parsedSensorLog.getCommInterval())
                .faultInformation(parsedSensorLog.getFaultInformation())
                .swVersion((long) parsedSensorLog.getSwVersion())
                .hwVersion((long) parsedSensorLog.getHwVersion())
                .buttonCount((long) parsedSensorLog.getButtonCount())
                .positionGuideCount((long) parsedSensorLog.getPositionGuideCount())
                .signalGuideCount((long) parsedSensorLog.getSignalGuideCount())
                .groupPositionNumber((long) parsedSensorLog.getGroupPositionNumber())
                .femaleMute1((long) parsedSensorLog.getSilentSettings().get("Female Mute 1"))
                .femaleMute2((long) parsedSensorLog.getSilentSettings().get("Female Mute 2"))
                .maleMute1((long) parsedSensorLog.getSilentSettings().get("Male Mute 1"))
                .maleMute2((long) parsedSensorLog.getSilentSettings().get("Male Mute 2"))
                .birdVolume((long) parsedSensorLog.getVolumeSettings().get("Bird Volume"))
                .cricketVolume((long) parsedSensorLog.getVolumeSettings().get("Cricket Volume"))
                .dingdongVolume((long) parsedSensorLog.getVolumeSettings().get("Dingdong Volume"))
                .femaleVolume((long) parsedSensorLog.getVolumeSettings().get("Female Volume"))
                .maleVolume((long) parsedSensorLog.getVolumeSettings().get("Male Volume"))
                .minuetVolume((long) parsedSensorLog.getVolumeSettings().get("Minuet Volume"))
                .systemVolume((long) parsedSensorLog.getVolumeSettings().get("System Volume"))
                .latitude(sensor.getLatitude())
                .longitude(sensor.getLongitude())
                .lastlyModifiedWith(sensorLogContentInstance)
                .serverTime(decodeServerTime(parsedSensorLog.getServerTime()))
                .updatedAt(LocalDateTime.now())
                .address(sensor.getAddress())
                .build();
        sensorRepository.save(updatedSensor);
    }

    public static LocalDateTime decodeServerTime(long serverTime) {
        // BCD에서 값 추출
        if(serverTime == 0) return null;
        int year = (int) ((serverTime >> 32) & 0xFF);  // YY
        int month = (int) ((serverTime >> 24) & 0xFF); // MM
        int day = (int) ((serverTime >> 16) & 0xFF);   // DD
        int hour = (int) ((serverTime >> 8) & 0xFF);   // HH
        int minute = (int) (serverTime & 0xFF);        // mm

        // 연도 변환 (2025년이면 25 → 2025)
        year += 2000;

        // 예외 처리: 월, 일, 시간, 분 값이 유효한 범위인지 확인
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month extracted from serverTime: " + month);
        }
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid day extracted from serverTime: " + day);
        }
        if (hour > 23) {
            throw new IllegalArgumentException("Invalid hour extracted from serverTime: " + hour);
        }
        if (minute > 59) {
            throw new IllegalArgumentException("Invalid minute extracted from serverTime: " + minute);
        }

        return LocalDateTime.of(year, month, day, hour, minute);
    }

    /**모든 센서의 위치 정보를 변환하여 string 값으로 추가*/
    @Transactional(readOnly = true)
    public void updateSensorAddress(){
        log.info("Sensor 위치 정보 조회 스케줄러 실행...");

        // 모든 센서 조회
        List<Sensor> sensors = sensorRepository.findAll();

        List<CompletableFuture<Void>> futures = sensors.stream()
            .map(sensor -> CompletableFuture.runAsync(() -> fetchAndLogLocation(sensor), executorService))
            .toList();

        // 모든 비동기 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    @Transactional
    protected void fetchAndLogLocation(Sensor sensor) {
        double longitude = sensor.getLongitude();
        double latitude = sensor.getLatitude();

        // 좌표 값이 0.0, 0.0이면 조회하지 않음
        if (longitude == 0.0 && latitude == 0.0) {
            return;
        }

        String url = String.format(KAKAO_API_URL, longitude, latitude);

        // Kakao HeaderProvider 사용
        String response = HttpClientUtil.get(url, new KakaoHeaderProvider("KakaoAK "+kakaoRestApiKey));

        if (response == null || response.isEmpty()) {
            return;
        }
        // API 응답에서 주소 추출
        String address = extractAddressFromResponse(response);
        updateSensorAddressInDB(sensor, address);
    }

    @Transactional
    protected void updateSensorAddressInDB(Sensor sensor, String address) {
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

    @Transactional
    public void archiveLogsBySensorDeviceNumber() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<SensorLog> logs = sensorLogRepository.findLogsOlderThan(cutoff);

        if (logs.isEmpty()) {
            log.info("보관할 로그 없음.");
            return;
        }

        // 1. 디바이스 + 날짜 기준으로 그룹핑
        Map<String, Map<LocalDate, List<SensorLog>>> grouped = logs.stream()
            .filter(log -> log.getSensorDeviceNumber() != null)
            .collect(Collectors.groupingBy(
                SensorLog::getSensorDeviceNumber,
                Collectors.groupingBy(log -> log.getCreatedAt().toLocalDate())
            ));

        // 2. 각 그룹(디바이스+날짜)별 파일 생성 및 업로드
        for (Map.Entry<String, Map<LocalDate, List<SensorLog>>> deviceEntry : grouped.entrySet()) {
            String deviceNumber = deviceEntry.getKey();

            for (Map.Entry<LocalDate, List<SensorLog>> dateEntry : deviceEntry.getValue().entrySet()) {
                LocalDate date = dateEntry.getKey();
                List<SensorLog> dateLogs = dateEntry.getValue();

                String filename = String.format("%s_%s.csv",
                        deviceNumber,
                        date.format(DateTimeFormatter.ofPattern("yyyyMMdd")));

                StringBuilder sb = getStringBuilder(dateLogs);

                try (InputStream inputStream = new ByteArrayInputStream(sb.toString().getBytes())) {
                    gcsUtil.uploadFileToGCS("sensor-log-archive", filename, inputStream, null);
                    sensorLogRepository.deleteAll(dateLogs);
                } catch (IOException e) {
                    log.error("❌ 업로드 실패 - {} ({})", deviceNumber, date, e);
                }
            }
        }
    }

    private StringBuilder getStringBuilder(List<SensorLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("id,sensor_group_id,event_code,event_details,sensor_device_number,is_processed,created_at\n");

        for (SensorLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(safeString(log.getSensorGroup().getId())).append(",");
            sb.append(safeString(log.getEventCode())).append(",");
            sb.append("\"").append(log.getEventDetails().replace("\"", "\"\"")).append("\",");
            sb.append(log.getSensorDeviceNumber()).append(",");
            sb.append("true").append(",");
            sb.append(log.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
        }

        return sb;
    }

    private String safeString(Object value) {
        return value == null ? "" : value.toString();
    }

    @PreDestroy
    public void shutdownExecutor() {
        log.info("🛑 Shutting down ExecutorService...");
        executorService.shutdown();
    }
}
