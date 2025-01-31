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
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/health","/_ah/warmup");
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
                        requests.requestMatchers(
                                "/account/sign-in", "/account/sign-up", "/project/*/job-notice",
                                        "/corporation/*/job-notices",
                                        "/corporation/recruit-page/*",
                                "/sms/send", "/sms/verify","/sms/verify-applicant/send",
                                "/applicant/sign-up", "/applicant/sign-in", "/applicant/reset-password", "/applicant/project/*/identification").permitAll()
                                .requestMatchers(HttpMethod.GET,"/announcement/**","/faq/**", "/corporation/recruit-page/*", "/corporation/*/job-notices", "/project/*/job-notice").permitAll()
                                .requestMatchers(SWAGGER_URIS).permitAll()
                                .requestMatchers(HttpMethod.GET, "/applicant/*/page/*/submit").authenticated()
                                .requestMatchers("/applicant/**").hasAuthority("applicant")
                                .requestMatchers("/project/*/step-date").permitAll()
                                .requestMatchers(HttpMethod.DELETE, "/project/*/applicant/*").permitAll()
                                .requestMatchers(HttpMethod.PATCH, "/project/*/applicant/*/step").permitAll()
                                .requestMatchers(HttpMethod.PATCH, "/project/**", "/corporation/**", "/announcement/**", "/faq/**", "/qna/**").hasAuthority("ADMIN")
                                .requestMatchers(HttpMethod.DELETE, "/project/**", "/corporation/**", "/announcement/**", "/faq/**", "/qna/**").hasAuthority("ADMIN")
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
