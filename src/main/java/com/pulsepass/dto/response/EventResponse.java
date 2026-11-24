package com.pulsepass.dto.response;

import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

// Campos: PRD Servicios §13
public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode,
        String venueName,
        // POR ACORDAR: el PRD pide "artists" sin exponer Set<Artist>, pero no fija el tipo
        List<ArtistResponse> artists
) {
}
