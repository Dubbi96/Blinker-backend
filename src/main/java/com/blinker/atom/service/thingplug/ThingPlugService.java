package com.blinker.atom.service.thingplug;

import com.blinker.atom.dto.thingplug.ContentInstanceRequestDto;
import com.blinker.atom.dto.thingplug.ParsedSensorLogDto;
import com.blinker.atom.util.EncodingUtil;
import com.blinker.atom.util.HttpClientUtil;
import com.blinker.atom.util.ParsingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
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

    public ParsedSensorLogDto getLatestContent(String remoteCseId) {
        // Step 1: Fetch Content Instance
        String listUrl = String.format("%s/%s/v1_0/remoteCSE-%s/container-LoRa/latest", baseUrl, appEui, remoteCseId);
        log.info("Fetching Content Instance list from URL: {}", listUrl);

        try {
            String latestInstanceResponse = HttpClientUtil.get(listUrl, origin, uKey, requestId);
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
        String response = HttpClientUtil.get(url, origin, uKey, requestId);

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

    public String createContentInstance(ContentInstanceRequestDto request) {
        String url = String.format("%s/%s/v1_0/remoteCSE-%s/container-%s", baseUrl, appEui, request.getRemoteCseId(), request.getContainerName());

        // XML Body for contentInstance
        String body = String.format(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<m2m:cin xmlns:m2m=\"http://www.onem2m.org/xml/protocols\">"
            + "<cnf>application/json</cnf>"
            + "<con>%s</con>"
            + "</m2m:cin>", EncodingUtil.encodeToHex(request));

        log.info("Creating contentInstance at URL: {}", url);
        log.info("Creating contentInstance: {}", body);
        return HttpClientUtil.post(url, origin, uKey, requestId, body);
    }

}