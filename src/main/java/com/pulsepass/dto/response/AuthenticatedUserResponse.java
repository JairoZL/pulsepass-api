package com.pulsepass.dto.response;

import java.util.List;

// PRD Seguridad FR-SEC-AUTH-010: GET /api/auth/me devuelve información MÍNIMA del principal.
// Sin hash, sin token, sin datos del perfil. roles sin prefijo: ["USER"], ["USER", "STAFF"]
public record AuthenticatedUserResponse(
        Long id,
        String username,
        List<String> roles
) {
}
