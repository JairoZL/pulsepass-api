package com.pulsepass.dto.request;

import com.pulsepass.model.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Campos: PRD Servicios §22. Validaciones: PRD Controladores §11.3
// No lleva precio: lo calcula el Service, no se confía en el cliente
public record PurchaseTicketRequest(

        // 180 = longitud de users.email (V1): se usa para buscar al usuario
        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        @Size(max = 180, message = "User email must not exceed 180 characters")
        String userEmail,

        // 30 = longitud de events.event_code (V1): se usa para buscar el evento
        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must not exceed 30 characters")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {
}
