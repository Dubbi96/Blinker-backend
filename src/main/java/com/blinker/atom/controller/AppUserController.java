package com.blinker.atom.controller;

import com.blinker.atom.dto.appuser.*;
import com.blinker.atom.service.appuser.AppUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService authService;

    @PostMapping("/sign-in")
    public SignInResponseDto login(@RequestBody SignInRequestDto authRequest) {
        log.info("로그인 요청 받음: {}", authRequest.getUsername());
        return authService.login(authRequest);
    }

    @PostMapping("/sign-up")
    public Long signup(@RequestBody SignUpRequestDto authRequestDto) {
        log.info("회원가입 요청 받음: {}", authRequestDto);
        return authService.signUp(authRequestDto);
    }

    @GetMapping("/user/{id}")
    public AppUserResponseDto getUserDetails(@PathVariable Long id) {
        return authService.getUserDetails(id);
    }

    @GetMapping("/users")
    public List<AppUsersResponseDto> getUserList() {
        return authService.getUserList();
    }

}