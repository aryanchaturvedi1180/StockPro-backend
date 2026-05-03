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

import java.util.List;
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

        User saved = User.builder()
                .id(1L)
                .fullName("Aryan")
                .email("aryan@stockpro.com")
                .phone("9876543210")
                .role(Role.STAFF)
                .passwordHash("hashed")
                .isActive(true)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = authService.register(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getRole()).isEqualTo(Role.STAFF);
        assertThat(response.getEmail()).isEqualTo("aryan@stockpro.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
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
    void registerShouldCreateAdminWithValidSecret() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@stockpro.com");
        request.setPassword("password123");
        request.setPhone("9999999999");
        request.setRole(Role.ADMIN);
        request.setAdminSecret("secret-admin");

        User saved = User.builder()
                .id(2L)
                .fullName("Admin User")
                .email("admin@stockpro.com")
                .phone("9999999999")
                .role(Role.ADMIN)
                .passwordHash("hashed")
                .isActive(true)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        var response = authService.register(request);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getRole()).isEqualTo(Role.ADMIN);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("aryan@stockpro.com");
        request.setRole(Role.STAFF);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    void loginShouldReturnJwtAndUpdateLastLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("manager@stockpro.com");
        request.setPassword("password123");

        User user = User.builder()
                .id(5L)
                .fullName("Manager")
                .email("manager@stockpro.com")
                .passwordHash("hash")
                .role(Role.MANAGER)
                .isActive(true)
                .build();

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

        User user = User.builder()
                .id(6L)
                .email("blocked@stockpro.com")
                .passwordHash("hash")
                .role(Role.STAFF)
                .isActive(false)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Account is deactivated");
    }

    @Test
    void loginShouldFailForWrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("manager@stockpro.com");
        request.setPassword("wrong");

        User user = User.builder()
                .id(5L)
                .email("manager@stockpro.com")
                .passwordHash("hash")
                .role(Role.MANAGER)
                .isActive(true)
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginShouldFailForUnknownEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@stockpro.com");
        request.setPassword("password123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void validateTokenShouldReturnFalseWhenTokenIsBlacklisted() {
        when(tokenBlacklistService.isBlacklisted("jwt-token")).thenReturn(true);

        boolean result = authService.validateToken("jwt-token");

        assertThat(result).isFalse();
        verify(jwtUtil, never()).isTokenValid(any());
    }

    @Test
    void validateTokenShouldReturnFalseWhenJwtInvalid() {
        when(tokenBlacklistService.isBlacklisted("jwt-token")).thenReturn(false);
        when(jwtUtil.isTokenValid("jwt-token")).thenReturn(false);

        boolean result = authService.validateToken("jwt-token");

        assertThat(result).isFalse();
    }

    @Test
    void getUserByIdShouldReturnUser() {
        User user = User.builder()
                .id(1L)
                .fullName("Aryan")
                .email("aryan@stockpro.com")
                .phone("9876543210")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        var response = authService.getUserById(1L);

        assertThat(response.getEmail()).isEqualTo("aryan@stockpro.com");
        assertThat(response.getFullName()).isEqualTo("Aryan");
    }

    @Test
    void getUserByIdShouldThrowWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAllUsersShouldReturnMappedUsers() {
        User user1 = User.builder()
                .id(1L)
                .fullName("Aryan")
                .email("aryan@stockpro.com")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        User user2 = User.builder()
                .id(2L)
                .fullName("Manager")
                .email("manager@stockpro.com")
                .role(Role.MANAGER)
                .isActive(true)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        var responses = authService.getAllUsers();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmail()).isEqualTo("aryan@stockpro.com");
        assertThat(responses.get(1).getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    void deactivateUserShouldDeactivateAndBlockUser() {
        User user = User.builder()
                .id(1L)
                .email("aryan@stockpro.com")
                .role(Role.STAFF)
                .isActive(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.deactivateUser(1L);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
        verify(tokenBlacklistService).blockUser(1L);
    }

    @Test
    void deactivateUserShouldThrowWhenUserMissing() {
        when(userRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.deactivateUser(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void reactivateUserShouldActivateAndUnblockUser() {
        User user = User.builder()
                .id(1L)
                .email("aryan@stockpro.com")
                .role(Role.STAFF)
                .isActive(false)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        authService.reactivateUser(1L);

        assertThat(user.isActive()).isTrue();
        verify(userRepository).save(user);
        verify(tokenBlacklistService).unblockUser(1L);
    }

    @Test
    void reactivateUserShouldThrowWhenUserMissing() {
        when(userRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.reactivateUser(100L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void logoutShouldBlacklistToken() {
        when(jwtUtil.getRemainingMillis("jwt-token")).thenReturn(5000L);

        authService.logout("jwt-token");

        verify(tokenBlacklistService).blacklist("jwt-token", 5000L);
    }
}