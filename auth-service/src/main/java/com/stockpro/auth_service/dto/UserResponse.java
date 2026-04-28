package com.stockpro.auth_service.dto;

import com.stockpro.auth_service.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private boolean isActive;
    private LocalDateTime createdAt;
}