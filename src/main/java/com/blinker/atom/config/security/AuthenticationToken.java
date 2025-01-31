package com.blinker.atom.config.security;

import java.util.Collection;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class AuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;

    private final String credentials;

    @Getter
    private final String type;

    // 인증되지 않은 객체
    public AuthenticationToken(String principal, String credentials, String type) {
        super(null);
        super.setAuthenticated(false);

        this.principal = principal;
        this.credentials = credentials;
        this.type = type;
    }

    // 인증된 객체
    AuthenticationToken(Object principal, String credentials, String type, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        super.setAuthenticated(true);

        this.principal = principal;
        this.credentials = credentials;
        this.type = type;
    }

    @Override
    public String getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

}
