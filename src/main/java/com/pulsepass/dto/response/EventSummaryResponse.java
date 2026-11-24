package com.pulsepass.dto.response;

import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;

import java.time.LocalDateTime;

// POR ACORDAR: el PRD no lista los campos; versión corta de EventResponse para listados
public record EventSummaryResponse(
        String eventCode,
        String name,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        String venueName
) {
}
