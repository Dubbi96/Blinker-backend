package com.blinker.atom.dto.appuser;

import com.blinker.atom.domain.appuser.AppUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AppUsersResponseDto {
    private Long appUserId;
    private String userId;
    private String username;

    public static AppUsersResponseDto fromEntity(AppUser user) {
        return new AppUsersResponseDto(user.getId(), user.getUserId(), user.getUsername());
    }
}
