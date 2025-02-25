package com.blinker.atom.util.httpclientutil;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;

@AllArgsConstructor
public class KakaoHeaderProvider implements HttpHeaderProvider{
    private final String authorization;

    @Override
    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        return headers;
    }
}
