package com.stockpro.auth_service.service;

import com.stockpro.auth_service.dto.*;
import com.stockpro.auth_service.entity.User;

import java.util.List;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    boolean validateToken(String token);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers();

    void deactivateUser(Long id);

    // Add to AuthService interface:
    void logout(String token);

    void reactivateUser(Long id);
}