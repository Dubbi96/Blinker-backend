package com.blinker.atom.config;

public final class Value {

    private Value() {
        throw new UnsupportedOperationException();
    }

    public static final String ACCESS_TOKEN_HEADER_KEY = "access-token";
    public static final String ACTIVE_PROFILE_KEY = "spring.profiles.active";
    public static final String PROD = "prod";
    public static final String LOCAL = "local";
    public static final String DEV = "dev";

}