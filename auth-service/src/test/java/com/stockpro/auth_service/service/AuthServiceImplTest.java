package com.stockpro.auth_service.service;

import com.stockpro.auth_service.dto.LoginRequest;
import com.stockpro.auth_service.dto.RegisterRequest;
import com.stockpro.auth_service.entity.Role;
import com.stockpro.auth_service.entity.User;
import com.stockpro.auth_service.repository.UserRepository;
import com.stockpro.auth_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "adminRegistrationSecret", "secret-admin");
    }

    @Test
    void registerShouldCreateStaffUserWithHashedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Aryan");
        request.setEmail("aryan@stockpro.com");
        request.setPassword("password123");
        request.setPhone("9876543210");
        request.setRole(Role.STAFF);

        User saved = User.builder().id(1L).fullName("Aryan").email("aryan@stockpro.com")
                .phone("9876543210").role(Role.STAFF).passwordHash("hashed").isActive(true).build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = authService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(Role.STAFF);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registerShouldRejectAdminWithoutSecret() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("admin@stockpro.com");
        request.setRole(Role.ADMIN);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid admin secret");

        verifyNoInteractions(userRepository);
    }

    @Test
    void loginShouldReturnJwtAndUpdateLastLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("manager@stockpro.com");
        request.setPassword("password123");

        User user = User.builder().id(5L).fullName("Manager").email("manager@stockpro.com")
                .passwordHash("hash").role(Role.MANAGER).isActive(true).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(5L, "manager@stockpro.com", "MANAGER")).thenReturn("jwt-token");

        var response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getRole()).isEqualTo("MANAGER");
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void loginShouldFailForInactiveUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("blocked@stockpro.com");
        request.setPassword("password123");
        User user = User.builder().id(6L).email("blocked@stockpro.com").passwordHash("hash").role(Role.STAFF).isActive(false).build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account is deactivated");
    }
}
