package com.blinker.atom.dto.appuser;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SignInRequestDto {
    @Schema(example = "admin")
    private String userId;
    @Schema(example = "admin")
    private String password;
}
