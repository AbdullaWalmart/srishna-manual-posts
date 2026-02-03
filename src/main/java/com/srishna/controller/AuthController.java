package com.srishna.controller;

import com.srishna.dto.AuthDto;
import com.srishna.entity.User;
import com.srishna.service.AuthService;
import com.srishna.service.JwtService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/signup")
    public ResponseEntity<AuthDto> signup(
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank @Size(min = 6) String password,
            @RequestParam(required = false) String name) {
        User user = authService.signup(email, password, name);
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return ResponseEntity.ok(AuthDto.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build());
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam @NotBlank String email,
            @RequestParam @NotBlank String password) {
        var result = authService.login(email, password);
        if (result.isPresent()) {
            return ResponseEntity.ok(result.get());
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgotPassword(@RequestParam @NotBlank @Email String email) {
        return authService.forgotPassword(email)
                .map(resetLink -> ResponseEntity.ok(Map.of("resetLink", resetLink)))
                .orElse(ResponseEntity.ok(Map.of("message", "If an account exists, a reset link has been sent.")));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(
            @RequestParam @NotBlank String token,
            @RequestParam @NotBlank @Size(min = 6) String newPassword) {
        boolean ok = authService.resetPassword(token, newPassword);
        if (!ok) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired reset token"));
        }
        return ResponseEntity.ok(Map.of("message", "Password reset successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDto> me(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).build();
        }
        String token = authHeader.substring(7);
        if (!jwtService.validateToken(token)) {
            return ResponseEntity.status(401).build();
        }
        Long userId = jwtService.getUserIdFromToken(token);
        return authService.findById(userId)
                .map(user -> ResponseEntity.ok(AuthDto.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .build()))
                .orElse(ResponseEntity.status(401).build());
    }
}
