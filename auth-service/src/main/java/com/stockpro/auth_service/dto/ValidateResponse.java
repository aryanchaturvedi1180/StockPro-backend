package com.stockpro.auth_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidateResponse {

    private boolean valid;
    private String message;
    private String email;
    private String role;
    private Long userId;
}