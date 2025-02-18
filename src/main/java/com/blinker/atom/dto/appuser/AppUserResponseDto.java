package com.blinker.atom.dto.appuser;

import com.blinker.atom.domain.appuser.Role;
import lombok.Data;

import java.util.List;

@Data
public class AppUserResponseDto {
    private Long userId;
    private String username;
    private String email;
    private List<Role> role;
    private boolean isActive;
}