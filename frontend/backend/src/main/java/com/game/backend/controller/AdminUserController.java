package com.game.backend.controller;

import com.game.backend.dto.AdminUserResponse;
import com.game.backend.dto.RoleChangeRequest;
import com.game.backend.dto.StatusChangeRequest;
import com.game.backend.entity.User;
import com.game.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<AdminUserResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(AdminUserResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        return AdminUserResponse.from(user);
    }

    @PatchMapping("/{id}/role")
    public AdminUserResponse changeRole(
            @PathVariable Long id,
            @RequestBody RoleChangeRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (!"USER".equals(request.getRole()) && !"ADMIN".equals(request.getRole())) {
            throw new RuntimeException("권한은 USER 또는 ADMIN만 가능합니다.");
        }

        user.setRole(request.getRole());

        return AdminUserResponse.from(userRepository.save(user));
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse changeStatus(
            @PathVariable Long id,
            @RequestBody StatusChangeRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        if (!"ACTIVE".equals(request.getStatus()) && !"BLOCKED".equals(request.getStatus())) {
            throw new RuntimeException("상태는 ACTIVE 또는 BLOCKED만 가능합니다.");
        }

        user.setStatus(request.getStatus());

        return AdminUserResponse.from(userRepository.save(user));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회원을 찾을 수 없습니다."));

        userRepository.delete(user);

        return Map.of("message", "회원이 삭제되었습니다.");
    }
}