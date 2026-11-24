package com.pulsepass.dto.response;

// POR ACORDAR: el PRD no lista los campos; se tomaron los de la entidad User.
// Falta decidir si incluye datos del perfil (firstName, lastName, ...)
public record UserResponse(
        Long id,
        String username,
        String email,
        Boolean active
) {
}
