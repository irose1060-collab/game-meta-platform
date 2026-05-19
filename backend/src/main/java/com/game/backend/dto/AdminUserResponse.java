package com.game.backend.dto;

import com.game.backend.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String role;
    private String status;
    private String provider;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .status(user.getStatus())
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
