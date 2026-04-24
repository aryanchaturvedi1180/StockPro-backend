package com.stockpro.auth_service.service;

import com.stockpro.auth_service.dto.*;
import com.stockpro.auth_service.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import com.stockpro.auth_service.entity.User;
import com.stockpro.auth_service.repository.UserRepository;
import com.stockpro.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${admin.registration.secret}")
    private String adminRegistrationSecret;

    @Override
    public UserResponse register(RegisterRequest request) {
        // ── Admin secret gate ──────────────────────────────────────────────
        if (request.getRole() == Role.ADMIN) {
            if (request.getAdminSecret() == null
                    || !request.getAdminSecret().equals(adminRegistrationSecret)) {
                throw new RuntimeException("Invalid admin secret. Admin registration not permitted.");
            }
        }
        // ──────────────────────────────────────────────────────────────────

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .build();

        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getEmail(), user.getRole().name(), user.getFullName());
    }

    @Override
    public boolean validateToken(String token) {
        if (tokenBlacklistService.isBlacklisted(token)) return false;       // logout blacklist
        if (!jwtUtil.isTokenValid(token)) return false;                      // expiry/signature

        // Check if the user was deactivated
        Long userId = jwtUtil.extractAllClaims(token).get("userId", Long.class);
        if (tokenBlacklistService.isUserBlocked(userId)) return false;

        return true;
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        return toResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setActive(false);
        userRepository.save(user);
        tokenBlacklistService.blockUser(id);  // Blocks active tokens immediately
    }

    @Override
    public void logout(String token) {
        long remaining = jwtUtil.getRemainingMillis(token);
        tokenBlacklistService.blacklist(token, remaining);
    }

    @Override
    public void reactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        user.setActive(true);
        userRepository.save(user);
        tokenBlacklistService.unblockUser(id);  // Allows login again
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}