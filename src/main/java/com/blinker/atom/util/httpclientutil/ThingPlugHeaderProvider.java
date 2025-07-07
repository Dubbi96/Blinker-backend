package com.blinker.atom.util.httpclientutil;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;


@AllArgsConstructor
public class ThingPlugHeaderProvider implements HttpHeaderProvider {
    private final String origin;
    private final String uKey;
    private final String requestId;

    @Override
    public HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/xml");
        headers.set("X-M2M-RI", requestId);
        headers.set("X-M2M-Origin", origin);
        headers.set("uKey", uKey);
        headers.set("locale", "ko");
        headers.setContentType(MediaType.parseMediaType("application/vnd.onem2m-res+xml"));
        return headers;
    }
}
