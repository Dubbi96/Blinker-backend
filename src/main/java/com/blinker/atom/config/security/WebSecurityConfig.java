package com.blinker.atom.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.blinker.atom.common.ApiResponse;
import com.blinker.atom.config.CodeValue;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.config.error.ExceptionHandlerFilter;
import java.io.PrintWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import retrofit2.http.GET;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final ExceptionHandlerFilter exceptionHandlerFilter;

    private static final String[] SWAGGER_URIS = {
            "/swagger-ui/**", "/api-docs",
            "/v3/api-docs/**", "/api-docs/**", "/swagger-ui.html",
            "/webjars/swagger-ui/**",
            "/account/signIn", "/account/signUp"
    };

    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, e) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            String json = objectMapper.writeValueAsString(
                    ApiResponse.builder()
                            .message(ErrorValue.ACCESS_DENIED.toString())
                            .code(CodeValue.ACCESS_DENIED.getValue())
                            .build());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            PrintWriter writer = response.getWriter();
            writer.write(json);
            writer.flush();
        };
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, e) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            String json = objectMapper.writeValueAsString(ApiResponse.builder()
                    .message(ErrorValue.UNAUTHORIZED.toString())
                    .code(CodeValue.NO_TOKEN_IN_REQUEST.getValue())
                    .build());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            PrintWriter writer = response.getWriter();
            writer.write(json);
            writer.flush();
        };
    }

    @Bean
    public HttpFirewall allowUrlEncodedSlashHttpFirewall() {
        DefaultHttpFirewall firewall = new DefaultHttpFirewall();
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public SecurityFilterChain filterChain (HttpSecurity http,
                                            JwtAuthenticationFilter jwtAuthenticationFilter,
                                            AccessDeniedHandler accessDeniedHandler,
                                            AuthenticationEntryPoint authenticationEntryPoint) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests ->
                        requests
                                .requestMatchers(SWAGGER_URIS).permitAll()
                                .requestMatchers("/auth/sign-in", "/auth/sign-up").permitAll()
                                .requestMatchers(HttpMethod.GET, "sensor/groups").hasAnyAuthority("ADMIN", "USER")
                                .requestMatchers(HttpMethod.GET, "sensor/groups/**", "auth/user/**", "auth/users").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "auth/user/**").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.PUT, "auth/user/password").hasAnyAuthority("ADMIN", "USER")
                                .requestMatchers(HttpMethod.PUT, "auth/user/**").hasAuthority("ADMIN")
                                .requestMatchers("scheduler/**").permitAll()
                                .requestMatchers(HttpMethod.POST, "/project/page/*/section", "/project/element", "/file/applicant/step/**", "/announcement/**", "/faq/**", "/qna/**").hasAuthority("ADMIN")
                                .requestMatchers("/project/**", "/corporation/**", "/qna/**", "/faq/**", "/file/**").hasAnyAuthority("VIEWER", "ADMIN")
                        .anyRequest().authenticated()
                ).cors(it -> {})
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(exceptionHandlerFilter, JwtAuthenticationFilter.class)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(it -> {
                    it.accessDeniedHandler(accessDeniedHandler);
                    it.authenticationEntryPoint(authenticationEntryPoint);
                })
                .httpBasic().disable()
                .build();
    }
}
