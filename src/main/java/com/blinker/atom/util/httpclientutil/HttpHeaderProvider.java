package com.blinker.atom.util.httpclientutil;

import org.springframework.http.HttpHeaders;

public interface HttpHeaderProvider {
     HttpHeaders createHeaders();
}
