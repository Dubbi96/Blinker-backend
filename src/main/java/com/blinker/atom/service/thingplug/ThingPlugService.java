package com.blinker.atom.service.thingplug;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.domain.sensor.Sensor;
import com.blinker.atom.domain.sensor.SensorGroup;
import com.blinker.atom.domain.sensor.SensorGroupRepository;
import com.blinker.atom.domain.sensor.SensorRepository;
import com.blinker.atom.dto.thingplug.SensorUpdateRequestDto;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.EncodingUtil;
import com.blinker.atom.util.httpclientutil.HttpClientUtil;
import com.blinker.atom.util.ParsingUtil;
import com.blinker.atom.util.httpclientutil.ThingPlugHeaderProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThingPlugService {

    private static final List<String> ORDERED_KEYS = Arrays.asList(
        "Gender", "Sound", "Crossroad" , "Proximity", "Configuration", "Priority"
    );

    private List<String> convertDeviceSettingsToList(Map<String, String> deviceSettings) {
        return ORDERED_KEYS.stream()
            .map(key -> deviceSettings.getOrDefault(key, ""))  // 값이 없으면 빈 문자열("")
            .collect(Collectors.toList());
    }


    private final SensorGroupRepository sensorGroupRepository;
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

    // 각 신호기(DeviceId)별 sequenceNumber 저장 (Thread-Safe)
    private final ConcurrentHashMap<Integer, Integer> sequenceNumbers = new ConcurrentHashMap<>();

    /**
     * 주어진 deviceId에 대한 sequenceNumber 증가 및 반환
     */
    public synchronized int getNextSequenceNumber(int deviceId) {
        // 현재 sequenceNumber 가져오기 (기본값 0)
        int currentSequence = sequenceNumbers.getOrDefault(deviceId, 0);

        // 0~255 범위 유지
        int nextSequence = (currentSequence + 1) % 256;

        // 업데이트 후 저장
        sequenceNumbers.put(deviceId, nextSequence);

        return nextSequence;
    }

    public ParsedSensorLogDto getLatestContent(String remoteCseId) {
        // Step 1: Fetch Content Instance
        String listUrl = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa/latest", baseUrl, appEui, remoteCseId);
        log.info("Fetching Content Instance list from URL: {}", listUrl);

        try {
            String latestInstanceResponse = HttpClientUtil.get(listUrl, new ThingPlugHeaderProvider(origin, uKey, requestId));
            log.debug("Content Instance List Response: {}", latestInstanceResponse);

            // Step 2: Extract and parse the <con> tag content
            String conData = extractContent(latestInstanceResponse);
            if (conData == null) {
                log.error("No <con> data found in Content Instance: {}", latestInstanceResponse);
                throw new IllegalArgumentException("No <con> data found.");
            }

            log.info("Extracted <con> data: {}", conData);
            return ParsingUtil.parseMessage(conData);

        } catch (HttpClientErrorException e) {
            log.error("HTTP Error during Content Instance processing. Status: {}, Error: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new IllegalArgumentException("400 Bad Request: Check remoteCSE ID or request parameters.");
            }
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Content Instance processing", e);
            throw e;
        }
    }

    private String extractContent(String response) {
        // Use regex to extract the <con> tag content
        Pattern pattern = Pattern.compile("<con>(.+?)</con>");
        Matcher matcher = pattern.matcher(response);
        return matcher.find() ? matcher.group(1) : null;
    }

    public List<String> fetchRemoteCSEIds() {
        String url = String.format("%s/%s/v1_0?fu=1&ty=16", baseUrl, appEui);
        String response = HttpClientUtil.get(url, new ThingPlugHeaderProvider(origin, uKey, requestId));
        List<String> remoteCSEIds = extractRemoteCSEIds(response);

        // 현재 존재하는 SensorGroup의 가장 큰 order 값
        Long maxOrder = sensorGroupRepository.findMaxOrder();
        long order = maxOrder == null ? 1 : maxOrder + 1;

        for (String sensorGroupId : remoteCSEIds) {
            if (!sensorGroupRepository.existsById(sensorGroupId)) {
                sensorGroupRepository.save(SensorGroup.builder()
                    .id(sensorGroupId)
                    .displayOrder(order++)
                    .build());
            }
        }

        return remoteCSEIds;
    }

    private List<String> extractRemoteCSEIds(String response) {
        List<String> remoteCSEIds = new ArrayList<>();
        String pattern = "/remoteCSE-([a-zA-Z0-9]+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(response);

        while (matcher.find()) {
            remoteCSEIds.add(matcher.group(1)); // 매칭된 ID만 추가
        }

        return remoteCSEIds;
    }

    public String updateSensorToThingPlug(String sensorGroupId, SensorUpdateRequestDto request) {
        String url = String.format("%s/%s/v1_0/mgmtCmd-%s_extDevMgmt", baseUrl, appEui, sensorGroupId);
        int sequenceNumber = getNextSequenceNumber(request.getDeviceId());
        String encodedContent = EncodingUtil.encodeToHex(request,83 , sequenceNumber);
        String body = String.format(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<m2m:mgc xmlns:m2m=\"http://www.onem2m.org/xml/protocols\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
        + "<exe>true</exe>"
        + "<exra>%s</exra>"
        + "</m2m:mgc>", encodedContent);
        Sensor sensor = sensorRepository.getSensorByDeviceNumberAndGroupPositionNumber(request.getDeviceNumber(),Long.parseLong(String.valueOf(request.getGroupPositionNumber())))
                .orElseThrow(() -> new CustomException("데이터베이스에 존재하지 않는 센서 입니다."));
        String response = HttpClientUtil.put(url, new ThingPlugHeaderProvider(origin, uKey, requestId), body);
        Sensor updatedSensor = Sensor.builder()
                .id(sensor.getId())
                .sensorGroup(sensor.getSensorGroup())
                .deviceNumber(request.getDeviceNumber())
                .deviceId((double) request.getDeviceId())
                .positionSignalStrength((long) request.getPositionSignalStrength())
                .positionSignalThreshold((long) request.getPositionSignalThreshold())
                .communicationSignalStrength((long) request.getCommunicationSignalStrength())
                .communicationSignalThreshold((long) request.getCommunicationSignalThreshold())
                .wireless235Strength((long) request.getWireless235Strength())
                .deviceSetting(convertDeviceSettingsToList(request.getDeviceSettings()))
                .communicationInterval((long) request.getCommunicationInterval())
                .faultInformation(sensor.getFaultInformation())
                .swVersion((long) request.getSwVersion())
                .hwVersion((long) request.getHwVersion())
                .buttonCount(sensor.getButtonCount())
                .positionGuideCount(sensor.getPositionGuideCount())
                .signalGuideCount(sensor.getSignalGuideCount())
                .groupPositionNumber((long) request.getGroupPositionNumber())
                .femaleMute1(request.getFemaleMute1())
                .femaleMute2(request.getFemaleMute2())
                .maleMute1(request.getMaleMute1())
                .maleMute2(request.getMaleMute2())
                .birdVolume(request.getBirdVolume())
                .cricketVolume(request.getCricketVolume())
                .dingdongVolume(request.getDingdongVolume())
                .femaleVolume(request.getFemaleVolume())
                .maleVolume(request.getMaleVolume())
                .minuetVolume(request.getMinuetVolume())
                .systemVolume(request.getSystemVolume())
                .latitude(sensor.getLatitude())
                .longitude(sensor.getLongitude())
                .lastlyModifiedWith(sensor.getLastlyModifiedWith())
                .serverTime(sensor.getServerTime())
                .updatedAt(sensor.getUpdatedAt())
                .address(sensor.getAddress())
                .build();
        sensorRepository.save(updatedSensor);
        return response;
    }

    public void sendFixedGCommand(String sensorGroupId) {
        String fixedHex = "470000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
        String url = String.format("%s/%s/v1_0/mgmtCmd-%s_extDevMgmt", baseUrl, appEui, sensorGroupId);

        String body = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<m2m:mgc xmlns:m2m=\"http://www.onem2m.org/xml/protocols\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
            + "<exe>true</exe>"
            + "<exra>%s</exra>"
            + "</m2m:mgc>", fixedHex);

        try {
            HttpClientUtil.put(url, new ThingPlugHeaderProvider(origin, uKey, requestId), body);
        } catch (Exception e) {
            throw new CustomException("ThingPlug 외부 요청에 실패하였습니다.");
        }
    }
}