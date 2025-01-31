package com.blinker.atom.dto;

import com.blinker.atom.domain.AppUser;
import com.blinker.atom.domain.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SignInResponseDto {
    private Long appUserId;
    private String accessToken;
    private List<Role> roles;

    public SignInResponseDto(AppUser appUser, String accessToken) {
        this.appUserId = appUser.getAppUserId();
        this.roles = appUser.getRoles();
        this.accessToken = accessToken;
    }
}
