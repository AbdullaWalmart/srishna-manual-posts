package com.srishna.service;

import com.srishna.entity.PasswordResetToken;
import com.srishna.entity.User;
import com.srishna.repository.PasswordResetTokenRepository;
import com.srishna.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final int RESET_TOKEN_VALID_HOURS = 24;

    @Transactional
    public User signup(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = User.builder()
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .name(name != null ? name.trim() : null)
                .build();
        return userRepository.save(user);
    }

    public Optional<com.srishna.dto.AuthDto> login(String email, String password) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
                .map(user -> {
                    String token = jwtService.generateToken(user.getId(), user.getEmail());
                    return com.srishna.dto.AuthDto.builder()
                            .token(token)
                            .id(user.getId())
                            .email(user.getEmail())
                            .name(user.getName())
                            .build();
                });
    }

    @Transactional
    public Optional<String> forgotPassword(String email) {
        return userRepository.findByEmail(email.trim().toLowerCase())
                .map(user -> {
                    String token = UUID.randomUUID().toString().replace("-", "");
                    Instant expires = Instant.now().plusSeconds(RESET_TOKEN_VALID_HOURS * 3600L);
                    resetTokenRepository.save(PasswordResetToken.builder()
                            .userId(user.getId())
                            .token(token)
                            .expiresAt(expires)
                            .used(false)
                            .build());
                    return baseUrl + "/reset?token=" + token;
                });
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> opt = resetTokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(token, Instant.now());
        if (opt.isEmpty()) return false;
        PasswordResetToken prt = opt.get();
        User user = userRepository.findById(prt.getUserId()).orElse(null);
        if (user == null) return false;
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        prt.setUsed(true);
        resetTokenRepository.save(prt);
        return true;
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
