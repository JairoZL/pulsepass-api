package com.pulsepass.dto.response;

import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Campos: PRD Servicios §23
public record TicketResponse(
        Long id,
        String ticketCode,
        TicketType type,
        BigDecimal price,
        TicketStatus status,
        LocalDateTime purchaseDate,
        String userEmail,
        String eventCode,
        String eventName
) {
}
