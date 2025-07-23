package com.blinker.atom.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Component
public class ConnectivityTester {

    @PostConstruct
    public void testConnectivity() {
        try {
            URL url = new URL("http://thingplugpf.sktiot.com:9000/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);  // 5초 타임아웃
            connection.connect();
            log.info("✅ ThingPlug 연결 성공: HTTP " + connection.getResponseCode());
        } catch (Exception e) {
            log.error("❌ ThingPlug 외부 연결 실패", e);
        }
    }
}