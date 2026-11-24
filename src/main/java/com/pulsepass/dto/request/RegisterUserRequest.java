package com.pulsepass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// Campos: PRD Servicios §19. Validaciones: PRD Controladores §11.2
public record RegisterUserRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        // phone y city son opcionales (columnas que aceptan NULL en user_profiles)
        String phone,

        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate
) {
}
