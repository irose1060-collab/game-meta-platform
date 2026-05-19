package com.game.backend.service;

import com.game.backend.dto.AuthResponse;
import com.game.backend.dto.LoginRequest;
import com.game.backend.dto.SignupRequest;
import com.game.backend.entity.RefreshToken;
import com.game.backend.entity.User;
import com.game.backend.repository.RefreshTokenRepository;
import com.game.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Transactional
    public void signup(SignupRequest request) {
        String email = request.getEmail().trim();
        String nickname = request.getNickname().trim();
        String password = request.getPassword();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("이미 사용 중인 이메일입니다.");
        }

        if (userRepository.existsByNickname(nickname)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        validatePassword(password);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .nickname(nickname)
                .role("USER")
                .status("ACTIVE")
                .provider("LOCAL")
                .build();

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다."));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new RuntimeException("소셜 로그인으로 가입된 계정입니다. Google 로그인을 이용해주세요.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        user.setLastLoginAt(LocalDateTime.now());

        return issueToken(userRepository.save(user));
    }

    @Transactional
    public AuthResponse oauthLoginOrSignup(
            String email,
            String nickname,
            String provider,
            String providerId,
            String profileImageUrl
    ) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("소셜 계정에서 이메일 정보를 가져오지 못했습니다.");
        }

        String safeEmail = email.trim();
        String safeNickname = makeUniqueNickname(nickname, safeEmail);

        User user = userRepository.findByEmail(safeEmail)
                .orElseGet(() -> User.builder()
                        .email(safeEmail)
                        .nickname(safeNickname)
                        .role("USER")
                        .status("ACTIVE")
                        .provider(provider)
                        .providerId(providerId)
                        .profileImageUrl(profileImageUrl)
                        .build());

        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(safeNickname);
        }

        user.setProvider(provider);
        user.setProviderId(providerId);
        user.setProfileImageUrl(profileImageUrl);
        user.setLastLoginAt(LocalDateTime.now());

        return issueToken(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse getCurrentUser(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효하지 않은 토큰입니다."));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("만료된 토큰입니다.");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return toAuthResponse(user, token);
    }

    @Transactional
    public void logout(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshTokenRepository::delete);
    }

    private AuthResponse issueToken(User user) {
        String token = UUID.randomUUID().toString();

        refreshTokenRepository.deleteByUserId(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(token)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        refreshTokenRepository.save(refreshToken);

        return toAuthResponse(user, token);
    }

    private AuthResponse toAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .token(token)
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .build();
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new RuntimeException("비밀번호는 8자 이상이어야 합니다.");
        }

        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*\\d.*");

        if (!hasLetter || !hasNumber) {
            throw new RuntimeException("비밀번호는 영문과 숫자를 포함해야 합니다.");
        }
    }

    private String makeUniqueNickname(String nickname, String email) {
        String base = nickname != null && !nickname.isBlank()
                ? nickname.trim()
                : email.split("@")[0];

        String candidate = base;
        int suffix = 1;

        while (userRepository.existsByNickname(candidate)) {
            candidate = base + suffix;
            suffix++;
        }

        return candidate;
    }
}
