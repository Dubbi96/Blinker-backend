package com.blinker.atom.service.scheduled;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.domain.sensor.*;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.HttpClientUtil;
import com.blinker.atom.util.ParsingUtil;
import com.blinker.atom.util.XmlUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final ObjectMapper objectMapper;
    private final SensorRepository sensorRepository;

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

    public static boolean IS_FETCH_SENSOR_LOG_RUNNING = false;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

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
    @Scheduled(fixedRate = 100000, initialDelay = 20000)  // 1일 1회 실행 (1000ms * 60 * 60 * 24 86400000)
    @Transactional(readOnly = true)
    public void fetchAndSaveSensorLogs() {
        if (!IS_FETCH_SENSOR_LOG_RUNNING) {
            return;
        }
        log.info("🔹 Sensor Log 스케줄러 실행 중...");
        // 모든 sensor_group 조회
        List<SensorGroup> sensorGroups = sensorGroupRepository.findAll();

        for (SensorGroup group : sensorGroups) {
            String sensorGroupId = group.getId();
            String url = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa?fu=1&ty=4",
                    baseUrl, appEui, sensorGroupId);

            String response = HttpClientUtil.get(url, origin, uKey, sensorGroupId);
            log.info("Fetching Sensor Log at URL: {}", response);
            if (response == null || response.isEmpty()) {
                log.warn("API 응답이 없음. SensorGroup: {}", sensorGroupId);
                continue;
            }

            // XML 파싱하여 contentInstance 리스트 추출
            List<String> eventCodes = extractContentInstanceUri(response);

            // SensorLog 저장 (중복 방지)
            saveSensorLogs(eventCodes, group);
        }
    }

    private List<String> extractContentInstanceUri(String response) {
        //extractContent(response);
        List<String> result = new ArrayList<>();
        Pattern pattern = Pattern.compile("/contentInstance-([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
            if (result.isEmpty()) {
                log.error("No valid Content Instance URI found in the response.");
            }
        return result;
    }

    private String extractContent(String response) {
        Pattern pattern = Pattern.compile("<con>(.+?)</con>");
        Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    @Transactional
    protected void saveSensorLogs(List<String> eventCodes, SensorGroup group) {
        List<CompletableFuture<Void>> futures = eventCodes.stream()
            .map(eventCode -> CompletableFuture.runAsync(() -> fetchAndSaveLog(eventCode, group), executorService))
            .toList();

        // 모든 요청이 완료될 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void fetchAndSaveLog(String eventCode, SensorGroup group) {
        if (sensorLogRepository.findByEventCode(eventCode).isPresent()) {
            log.info("이미 존재하는 이벤트 코드 (스킵): {}", eventCode);
            return;
        }

        String contentInstanceUrl = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa/contentInstance-%s",
                baseUrl, appEui, group.getId(), eventCode);

        try {
            String contentInstanceResponse = HttpClientUtil.get(contentInstanceUrl, origin, uKey, requestId);
            String jsonEventDetail = XmlUtil.convertXmlToJson(contentInstanceResponse);

            SensorLog logEntry = SensorLog.builder()
                    .sensorGroup(group)
                    .eventCode(eventCode)
                    .eventDetails(jsonEventDetail)
                    .build();

            sensorLogRepository.save(logEntry);
            parseSensorLog(logEntry, group);
            log.info("✅ SensorLog 저장 완료: {}", eventCode);
        } catch (HttpClientErrorException e) {
            log.error("HTTP Error during Content Instance processing. Status: {}, Error: {}",
                        e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException("400 Bad Request: Check remoteCSE ID or request parameters.");
            }
            throw e;
       } catch (JsonProcessingException e) {
            log.error("Unexpected error during Content Instance processing", e);
            throw new CustomException("Unexpected error during Content Instance processing");
        }
    }

    /*@Transactional
    protected void saveSensorLogs(List<String> eventCodes, SensorGroup group) {
        for (String eventCode : eventCodes) {
            // 이미 존재하는 eventCode인지 확인
            if (sensorLogRepository.findByEventCode(eventCode).isPresent()) {
                log.info("이미 존재하는 이벤트 코드 (스킵): {}", eventCode);
                continue;
            }

            String contentInstanceUrl = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa/contentInstance-%s", baseUrl, appEui, group.getId() ,eventCode);
            try{
                String contentInstanceResponse = HttpClientUtil.get(contentInstanceUrl, origin, uKey, requestId);
                String jsonEventDetail = XmlUtil.convertXmlToJson(contentInstanceResponse);
                // SensorLog 저장
                SensorLog logEntry = SensorLog.builder()
                    .sensorGroup(group)
                    .eventCode(eventCode)
                    .eventDetails(jsonEventDetail)
                    .build();

                SensorLog sensorLog = sensorLogRepository.save(logEntry);
                parseSensorLog(sensorLog, group);
                log.info("SensorLog 저장 완료: {}", eventCode);

            }catch (HttpClientErrorException e) {
                log.error("HTTP Error during Content Instance processing. Status: {}, Error: {}",
                            e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    throw new IllegalArgumentException("400 Bad Request: Check remoteCSE ID or request parameters.");
                }
                throw e;
           } catch (JsonProcessingException e) {
                log.error("Unexpected error during Content Instance processing", e);
                throw new CustomException("Unexpected error during Content Instance processing");
            }
        }
    }*/

    @Transactional
    public void parseSensorLog(SensorLog sensorLog, SensorGroup group) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(sensorLog.getEventDetails());
        // con 태그 분리
        String sensorLogContentInstance = jsonNode.get("con").asText();
        ParsedSensorLogDto parsedSensorLog = ParsingUtil.parseMessage(sensorLogContentInstance);

        // event log에서 created time 추출 & 변환
        String sensorLogCreatedTimeAsString = jsonNode.get("ct").asText();
        LocalDateTime sensorLogCreatedTime = OffsetDateTime.parse(sensorLogCreatedTimeAsString, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime();

        if(parsedSensorLog.getCmd().equals("67") || parsedSensorLog.getCmd().equals("73")) {
            // event log에서 lat, long 정보 추출
            List<String> sensorLogLatitudeAndLongitudeAsString = List.of(jsonNode.get("ppt").get("gwl").asText().split(","));
            // Device Number로 Sensor 찾기
            Optional<Sensor> optionalSensor = sensorRepository.findSensorByDeviceNumber(parsedSensorLog.getDeviceNumber());
            if (optionalSensor.isPresent()) {
                Sensor existingSensor = optionalSensor.get();
                // 기존 센서의 최신 로그 시간이 현재 로그 시간보다 크거나 같으면 저장하지 않음
                if (existingSensor.getUpdatedAt() != null && existingSensor.getUpdatedAt().isAfter(sensorLogCreatedTime) || existingSensor.getUpdatedAt() != null && existingSensor.getUpdatedAt().isEqual(sensorLogCreatedTime)) {
                    return;
                }
                // 센서 업데이트 (최신 정보 반영)
                updateSensor(existingSensor, parsedSensorLog, sensorLogLatitudeAndLongitudeAsString, sensorLogContentInstance);
                sensorRepository.save(existingSensor);
                log.info(" 🆙 센서정보 업데이트 됨 : {} ", existingSensor.getDeviceNumber());
            } else {
                // 새로운 센서 저장
                Sensor newSensor = createNewSensor(parsedSensorLog, sensorLog.getSensorGroup(), sensorLogLatitudeAndLongitudeAsString, sensorLogContentInstance);
                log.info(" 🆕 새로운 센서 저장 됨 : {} ", newSensor.getDeviceNumber());
            }
            //sensor group의 sensor count, sensor_group_key, fault_count 체크
            if (group.getUpdatedAt() != null && group.getUpdatedAt().isAfter(sensorLogCreatedTime) || group.getUpdatedAt() != null && group.getUpdatedAt().isEqual(sensorLogCreatedTime)) return;
            sensorGroupRepository.updateSensorGroup(group.getId(), parsedSensorLog.getGroupNumber(), parsedSensorLog.getSignalsInGroup());
        }
        if(parsedSensorLog.getCmd().equals("61") || parsedSensorLog.getCmd().equals("77")) {
            if (group.getSsidUpdatedAt() != null && group.getSsidUpdatedAt().isAfter(sensorLogCreatedTime) || group.getSsidUpdatedAt() != null && group.getSsidUpdatedAt().isEqual(sensorLogCreatedTime)) return;
            sensorGroupRepository.updateSsid(group.getId(),sensorLogContentInstance.substring(2,16));
        }
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
                .latitude(Double.parseDouble(sensorLogLatitudeAndLongitudeAsString.get(0)))
                .longitude(Double.parseDouble(sensorLogLatitudeAndLongitudeAsString.get(1)))
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
                .latitude(Double.parseDouble(sensorLogLatitudeAndLongitudeAsString.get(0)))
                .longitude(Double.parseDouble(sensorLogLatitudeAndLongitudeAsString.get(1)))
                .lastlyModifiedWith(sensorLogContentInstance)
                .updatedAt(LocalDateTime.now())
                .build();
        sensorRepository.save(updatedSensor);
    }

    public void startScheduler() {
        IS_FETCH_SENSOR_LOG_RUNNING = true;
        log.info("✅ 스케줄러가 활성화되었습니다.");
    }

    public void stopScheduler() {
        IS_FETCH_SENSOR_LOG_RUNNING = false;
        log.info("⛔ 스케줄러가 비활성화되었습니다.");
    }
}
