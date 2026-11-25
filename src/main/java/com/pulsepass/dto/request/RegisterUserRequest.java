package com.pulsepass.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

// Campos: PRD Servicios §19. Validaciones: PRD Controladores §11.2
// Límites @Size: columnas de users y user_profiles en V1
// password: PRD Seguridad §3.4 y §9.3 (cambio de contrato documentado; nunca se devuelve en UserResponse)
public record RegisterUserRequest(

        @NotBlank(message = "Username is required")
        @Size(max = 50, message = "Username must not exceed 50 characters")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 180, message = "Email must not exceed 180 characters")
        String email,

        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name must not exceed 80 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name must not exceed 80 characters")
        String lastName,

        // phone y city son opcionales (columnas que aceptan NULL en user_profiles)
        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotNull(message = "Birth date is required")
        LocalDate birthDate,

        // Mínimo académico de 12 caracteres (§9.3)
        @NotBlank(message = "Password is required")
        @Size(min = 12, message = "Password must have at least 12 characters")
        String password
) {
    // BCrypt solo usa los primeros 72 BYTES (§9.3). @Size cuenta caracteres, no bytes:
    // "ñ" o "á" ocupan 2 bytes en UTF-8. Bean Validation también valida los métodos isXxx(),
    // así que este error sale en details como "passwordWithinBcryptLimit"
    @AssertTrue(message = "Password must not exceed 72 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // El toString automático de un record imprimiría la contraseña si el request llegara a un log
    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username + ", email=" + email + ", password=****]";
    }
}
