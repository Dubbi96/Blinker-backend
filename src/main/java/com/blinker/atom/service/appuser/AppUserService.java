package com.blinker.atom.service.appuser;

import com.blinker.atom.config.error.CustomException;
import com.blinker.atom.config.error.ErrorValue;
import com.blinker.atom.config.security.JwtProvider;
import com.blinker.atom.config.security.LoginAppUser;
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
        if (!passwordEncoder.matches(accountRequestDto.getPassword() + appUser.getSalt(), appUser.getPassword()))
            throw new CustomException("올바르지 않은 아이디 및 비밀번호입니다.");
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
                .build();

        appUserRepository.save(newUser);
        return newUser.getId();
    }

    @Transactional(readOnly = true)
    public AppUserResponseDto getUserDetails(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        return new AppUserResponseDto(user);
    }

    @Transactional(readOnly = true)
    public List<AppUserResponseDto> getUserList() {
        return appUserRepository.findAll()
                .stream()
                .sorted(Comparator.comparing((AppUser user) -> user.getRoles().contains(Role.ADMIN) ? 0 : 1) // ADMIN 우선 정렬
                        .thenComparing(AppUser::getId)) // 같은 역할 내에서 appUserId 오름차순 정렬
                .map(AppUserResponseDto::new)
                .toList();
    }

    @Transactional
    public void updateAppUserPassword(AppUser appUser, String newPassword) {
        String salt = UUID.randomUUID().toString();
        String encodedPassword = passwordEncoder.encode(newPassword + salt);
        appUser.updatePassword(encodedPassword, salt);
        appUserRepository.save(appUser);
    }

    @Transactional
    public void deleteAppUserWithRoleOfUser(AppUser appUser, Long appUserId) {
        AppUser pendingAppUser = appUserRepository.findAppUserById(appUserId).orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        if (pendingAppUser.getRoles().contains(Role.ADMIN) && !appUser.getId().equals(pendingAppUser.getId())) {
            throw new CustomException("ADMIN 계정은 자신만 삭제할 수 있습니다. 삭제하고자 하는 계정의 권한 : " + pendingAppUser.getRoles().get(0));
        }
        appUserRepository.deleteAppUserById(pendingAppUser.getId());
    }

    @Transactional
    public void updateAppUserStatus(AppUser appUser, Long appUserId, AppUserStatusUpdateRequestDto appUserStatusUpdateRequestDto) {
        AppUser pendingAppUser = appUserRepository.findAppUserById(appUserId).orElseThrow(() -> new CustomException(ErrorValue.ACCOUNT_NOT_FOUND.getMessage()));
        if (pendingAppUser.getRoles().contains(Role.ADMIN) && !appUser.getId().equals(pendingAppUser.getId())) {
            throw new CustomException("ADMIN 계정은 자신만 수정할 수 있습니다. 수정하고자 하는 계정의 권한 : " + pendingAppUser.getRoles().get(0));
        }
        pendingAppUser.updateStatus(appUserStatusUpdateRequestDto.getUserId(), appUserStatusUpdateRequestDto.getUsername());
        appUserRepository.save(pendingAppUser);
    }
}