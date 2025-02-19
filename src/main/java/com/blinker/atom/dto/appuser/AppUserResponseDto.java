package com.blinker.atom.dto.appuser;

import com.blinker.atom.domain.appuser.AppUser;
import com.blinker.atom.domain.appuser.Role;
import lombok.Data;

import java.util.List;

@Data
public class AppUserResponseDto {
    private Long appUserId;
    private String userId;
    private String username;

    public AppUserResponseDto(AppUser user) {
        this.appUserId = user.getId();
        this.userId = user.getUserId();
        this.username = user.getUsername();
    }
}