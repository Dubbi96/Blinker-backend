package com.blinker.atom.service.thingplug;

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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThingPlugService {

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

        return extractRemoteCSEIds(response);
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

    public String createContentInstance(String sensorGroupId, SensorUpdateRequestDto request) {
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

        log.info("Creating contentInstance at URL: {}", url);
        log.info("Creating contentInstance: {}", body);
        return HttpClientUtil.put(url, new ThingPlugHeaderProvider(origin, uKey, requestId), body);
    }

}