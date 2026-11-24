package com.pulsepass.dto.request;

import com.pulsepass.model.EventCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// Campos: PRD Servicios §12. Validaciones: PRD Controladores §11.1
public record CreateEventRequest(

        // 30 = longitud de la columna events.event_code (V1)
        @NotBlank(message = "Event code is required")
        @Size(max = 30, message = "Event code must not exceed 30 characters")
        String eventCode,

        // 80 = longitud de la columna events.name (V1)
        @NotBlank(message = "Name is required")
        @Size(max = 80, message = "Name must not exceed 80 characters")
        String name,

        // Obligatorio: events.description es NOT NULL. 500 = longitud de la columna (V1)
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotNull(message = "Event date is required")
        LocalDateTime eventDate,

        @NotNull(message = "Minimum age is required")
        @Min(value = 0, message = "Minimum age must be 0 or greater")
        Integer minimumAge,

        // 30 = longitud de venues.code (V1): se usa para buscar el venue
        @NotBlank(message = "Venue code is required")
        @Size(max = 30, message = "Venue code must not exceed 30 characters")
        String venueCode
) {
}
