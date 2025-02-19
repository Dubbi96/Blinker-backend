package com.blinker.atom.service.health;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostConstructService {

    @PostConstruct
    public void init() {
        log.warn("post construct successful");
    }
}
