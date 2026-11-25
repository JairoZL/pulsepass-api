package com.pulsepass.dto.response;

// PRD Seguridad §10.1 y §10.2: respuesta del login.
// tokenType = "Bearer" y expiresIn en segundos (900 = 15 minutos)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
