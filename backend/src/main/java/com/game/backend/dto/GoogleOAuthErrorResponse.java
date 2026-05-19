package com.game.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GoogleOAuthErrorResponse {
    private String error;
    private String message;
}
