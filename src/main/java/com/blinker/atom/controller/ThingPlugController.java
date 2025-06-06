package com.blinker.atom.controller;

import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.service.thingplug.ThingPlugService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/skt")
public class ThingPlugController {

    private final ThingPlugService thingPlugService;

    @PostMapping(consumes = {"application/json", "application/xml"})
    public ResponseEntity<String> receiveThingPlugMessage(@RequestBody String payload,
                                                          @RequestHeader(value = "Content-Type") String contentType) {
        try {
            String data;
            if (contentType.contains("json")) {
                // ✅ JSON 메시지 처리
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(payload);
                data = jsonNode.get("pc").get("cin").get("con").asText();
            } else if (contentType.contains("xml")) {
                // ✅ XML 메시지 처리
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(payload)));

                data = doc.getElementsByTagName("con").item(0).getTextContent();
            } else {
                log.error("❌ 지원되지 않는 Content-Type: " + contentType);
                return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body("Unsupported Content-Type");
            }

            log.info("📌 수신된 데이터: " + data);

            // ✅ 데이터베이스 저장 로직 추가 가능
            return ResponseEntity.status(HttpStatus.CREATED).body("Received");

        } catch (Exception e) {
            log.error("❌ 메시지 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Parsing Error");
        }
    }

    @GetMapping("/{sensorId}/latest")
    public ParsedSensorLogDto getLatestContent(@PathVariable String sensorId) {
        try {
            // 1. 가장 최근 Content Instance 가져오기

            /** 2. 모든 Content Instance 가져오기
            String allContentInstances = thingPlugService.getAllContentInstances(remoteCseId);
             **/
            return thingPlugService.getLatestContent(sensorId);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    @GetMapping("/remoteCSEs")
    public List<String> getRemoteCSEIds() {
        return thingPlugService.fetchRemoteCSEIds();
    }
}
