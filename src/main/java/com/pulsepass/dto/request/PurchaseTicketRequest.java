package com.pulsepass.dto.request;

import com.pulsepass.model.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Campos: PRD Servicios §22. Validaciones: PRD Controladores §11.3
// No lleva precio: lo calcula el Service, no se confía en el cliente
public record PurchaseTicketRequest(

        @NotBlank(message = "User email is required")
        @Email(message = "User email must be valid")
        String userEmail,

        @NotBlank(message = "Event code is required")
        String eventCode,

        @NotNull(message = "Ticket type is required")
        TicketType type
) {
}
