package com.blinker.atom.config.security;

import com.blinker.atom.config.Value;
import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.domain.AppUser;
import com.blinker.atom.domain.AppUserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@RequiredArgsConstructor
@Component
/*
    Request가 Controller까지 도착하기 전, 필터에서 헤더를 체크하여 access token이 있다면 검증을 진행한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final AppUserRepository appUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            log.debug("SecurityContextHolder는 이미 authentication 객체를 가지고 있습니다.: '{}'",
                    SecurityContextHolder.getContext().getAuthentication());
            chain.doFilter(request, response);
            return;
        }
        String jwtToken = request.getHeader(Value.ACCESS_TOKEN_HEADER_KEY);
        if (isNull(jwtToken)) {
            chain.doFilter(request, response);
            return;
        }
        Claims claims = jwtProvider.verifyToken(jwtToken);
        AuthenticationToken authenticationToken = authenticate(claims);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        chain.doFilter(request, response);
    }
    private AuthenticationToken authenticate(Claims claims) {
        if (claims.get("appUserId") != null) {
            return authenticateAppUser(Long.parseLong(claims.get("appUserId").toString()));
        }
        throw new IllegalArgumentException("유효하지 않은 토큰 : 인증된 사용자 타입이 아닙니다.");
    }

    private AuthenticationToken authenticateAppUser(Long appUserId) {
        AppUser appUser = appUserRepository.findById(appUserId)
                .orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        TokenPrincipal principal = new TokenPrincipal(appUser);
        List<SimpleGrantedAuthority> roles = appUser.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority(r.name()))
                .toList();
        return new AuthenticationToken(principal, null, "appUser", roles);
    }
}
