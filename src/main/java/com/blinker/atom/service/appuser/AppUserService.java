package com.blinker.atom.service.appuser;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.config.security.JwtProvider;
import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.AppUserRepository;
import com.blinker.atom.domain.appuser.Role;
import com.blinker.atom.dto.appuser.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public SignInResponseDto login(SignInRequestDto accountRequestDto) {
        AppUser appUser = (AppUser) appUserRepository.findByUserId(accountRequestDto.getUsername())
                .orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        if (!passwordEncoder.matches(accountRequestDto.getPassword() + appUser.getSalt(), appUser.getPassword())) throw new CustomException("올바르지 않은 아이디 및 비밀번호입니다.");
        return new SignInResponseDto(appUser, jwtProvider.createAccessToken(appUser.getId()));
    }

    @Transactional
    public Long signUp(SignUpRequestDto authRequestDto) {
        appUserRepository.findByUsername(authRequestDto.getUserId()).ifPresent(appUser -> {
            throw new IllegalArgumentException(ErrorValue.NICKNAME_ALREADY_EXISTS.toString());
        });
        String salt = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(authRequestDto.getPassword() + salt);

        Role userRole = Role.valueOf(authRequestDto.getRole().toUpperCase()); // String을 Role enum으로 변환

        AppUser newUser = AppUser.builder()
                .userId(authRequestDto.getUserId())
                .username(authRequestDto.getUsername())
                .password(encodedPassword)
                .salt(salt)
                .roles(Collections.singletonList(userRole))
                .isActive(true)
                .build();

        appUserRepository.save(newUser);
        return newUser.getId();
    }

    @Transactional(readOnly = true)
    public AppUserResponseDto getUserDetails(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자가 존재하지 않습니다."));
        AppUserResponseDto userResponse = new AppUserResponseDto();
        userResponse.setUserId(user.getId());
        userResponse.setUsername(user.getUserId());
        userResponse.setRole(user.getRoles());
        return userResponse;
    }

    @Transactional(readOnly = true)
    public List<AppUsersResponseDto> getUserList() {
        return appUserRepository.findAll()
                .stream()
                .map(AppUsersResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
}