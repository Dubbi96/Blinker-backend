package com.blinker.atom.controller;

import com.blinker.atom.config.security.LoginAppUser;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.dto.appuser.*;
import com.blinker.atom.service.appuser.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
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

    @GetMapping("/user/{appUserId}")
    @Operation(summary = "단일 유저 정보 조회 ⭐️Admin 전용", description = "appUserId에 해당하는 유저 정보 조회")
    public AppUserResponseDto getUserDetails(@PathVariable("appUserId") Long appUserId) {
        return authService.getUserDetails(appUserId);
    }

    @GetMapping("/users")
    @Operation(summary = "모든 유저 정보 조회 ⭐️Admin 전용", description = "모든 AppUser에 대한 정보 목록 조회 📌 정렬 기준 : (1) Admin 그룹 우선 노출 (2) AppUserId 순으로 오름차순 정렬")
    public List<AppUserResponseDto> getUserList() {
        return authService.getUserList();
    }

    @PutMapping("/user/password")
    @Operation(summary = "유저 비밀번호 변경", description = "특정 AppUser에 대하여 신규 비밀번호를 설정, 비밀번호 변경 시 salt도 재설정")
    public void updateUserPassword(@LoginAppUser AppUser appUser, @RequestBody String newPassword){
        authService.updateAppUserPassword(appUser, newPassword);
    }

    @DeleteMapping("/user/{appUserId}")
    @Operation(summary = "단일 유저 삭제 ⭐️Admin 전용", description = "appUserId에 해당하는 유저 삭제 ⚠️ 만약 타 ADMIN 권한을 가진 계정을 삭제할 경우 DIALOG 반환")
    public void deleteUser(@LoginAppUser AppUser appUser, @PathVariable("appUserId") Long appUserId) {
        authService.deleteAppUserWithRoleOfUser(appUser, appUserId);
    }

    @PutMapping("/user/{appUserId}")
    @Operation(summary = "단일 유저 정보 수정 ⭐️Admin 전용", description = "appUserId에 해당하는 유저의 아이디, 사용자명 수정 ⚠️ 만약 타 ADMIN 권한을 가진 계정을 수정할 경우 DIALOG 반환")
    public void updateUserStatus(@LoginAppUser AppUser appUser, @PathVariable("appUserId") Long appUserId, @RequestBody AppUserStatusUpdateRequestDto appUserStatusUpdateRequestDto) {
        authService.updateAppUserStatus(appUser, appUserId, appUserStatusUpdateRequestDto);
    }
}