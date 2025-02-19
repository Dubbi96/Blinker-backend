package com.blinker.atom.dto.appuser;

import lombok.Data;

@Data
public class AppUserStatusUpdateRequestDto {
    private String userId;
    private String username;
}
