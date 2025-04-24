package com.blinker.atom.dto.appuser;

import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SignInResponseDto {
    private Long appUserId;
    private String userId;
    private String accessToken;
    private List<Role> roles;

    public SignInResponseDto(AppUser appUser, String accessToken) {
        this.appUserId = appUser.getId();
        this.userId = appUser.getUserId();
        this.roles = appUser.getRoles();
        this.accessToken = accessToken;
    }
}
