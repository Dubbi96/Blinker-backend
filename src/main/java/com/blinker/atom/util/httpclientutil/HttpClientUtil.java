package com.blinker.atom.util.httpclientutil;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class HttpClientUtil {

    public static String get(String url, HttpHeaderProvider headerProvider) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = headerProvider.createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return response.getBody();
    }

    public static String post(String url, HttpHeaderProvider headerProvider, String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = headerProvider.createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        return response.getBody();
    }

    public static String put(String url, HttpHeaderProvider headerProvider, String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = headerProvider.createHeaders();
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        return response.getBody();
    }
}