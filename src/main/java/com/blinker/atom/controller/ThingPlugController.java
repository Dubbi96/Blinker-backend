package com.blinker.atom.controller;

import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.service.thingplug.ThingPlugService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/skt")
public class ThingPlugController {

    private final ThingPlugService thingPlugService;

    @PostMapping("/subscription-test")
    public void subscriptionTest(@RequestBody String payload) {
        log.error("📩 ThingPlug에서 메시지 수신: " + payload);

        // 수신된 메시지 파싱
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(payload);
            String data = jsonNode.get("pc").get("cin").get("con").asText();
            log.error("📌 수신된 데이터: " + data);

            // 데이터베이스 저장 로직 추가 가능
        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생", e);
        }
    }

    @GetMapping("/{sensorId}/latest")
    public ParsedSensorLogDto getLatestContent(@PathVariable String sensorId) {
        try {
            // 1. 가장 최근 Content Instance 가져오기
            ParsedSensorLogDto parsedData = thingPlugService.getLatestContent(sensorId);

            /** 2. 모든 Content Instance 가져오기
            String allContentInstances = thingPlugService.getAllContentInstances(remoteCseId);
             **/
            return parsedData;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @GetMapping("/remoteCSEs")
    public List<String> getRemoteCSEIds() {
        return thingPlugService.fetchRemoteCSEIds();
    }

}
