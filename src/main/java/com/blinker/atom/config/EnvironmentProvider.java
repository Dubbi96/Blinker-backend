package com.blinker.atom.config;

import static java.util.Objects.nonNull;

import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class EnvironmentProvider {

    private final Environment environment;

    public boolean isProd() {
        return nonNull(environment.getProperty(Value.ACTIVE_PROFILE_KEY)) && Objects.equals(
                environment.getProperty(Value.ACTIVE_PROFILE_KEY), Value.PROD);
    }

    public boolean isDev() {
        return nonNull(environment.getProperty(Value.ACTIVE_PROFILE_KEY)) && Objects.equals(
                environment.getProperty(Value.ACTIVE_PROFILE_KEY), Value.DEV);
    }

    public boolean isLocal() {
        return nonNull(environment.getProperty(Value.ACTIVE_PROFILE_KEY)) && Objects.equals(
                environment.getProperty(Value.ACTIVE_PROFILE_KEY), Value.LOCAL);
    }
}
