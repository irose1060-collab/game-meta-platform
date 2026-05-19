package com.game.backend.controller;

import com.game.backend.dto.AuthResponse;
import com.game.backend.dto.LoginRequest;
import com.game.backend.dto.SignupRequest;
import com.game.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "사용 가능한 이메일입니다." : "이미 사용 중인 이메일입니다."
        ));
    }

    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Object>> checkNickname(@RequestParam String nickname) {
        boolean available = authService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available ? "사용 가능한 닉네임입니다." : "이미 사용 중인 닉네임입니다."
        ));
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(Map.of("message", "회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return ResponseEntity.ok(authService.getCurrentUser(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestHeader(value = "Authorization", required = false) String token
    ) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "로그아웃이 완료되었습니다."));
    }
}
