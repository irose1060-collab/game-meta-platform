package com.game.backend.dto;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String token;
    private String provider;
    private String profileImageUrl;
}
