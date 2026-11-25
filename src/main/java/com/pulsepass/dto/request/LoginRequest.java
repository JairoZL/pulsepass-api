package com.pulsepass.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// PRD Seguridad §10.1: POST /api/auth/login. identifier acepta username o correo (FR-SEC-AUTH-002)
public record LoginRequest(

        // 180 = el más largo entre users.username (50) y users.email (180)
        @NotBlank(message = "Identifier is required")
        @Size(max = 180, message = "Identifier must not exceed 180 characters")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
    // El toString automático de un record imprimiría la contraseña si el request llegara a un log
    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + ", password=****]";
    }
}
