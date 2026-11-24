package com.pulsepass.dto.response;

// POR ACORDAR: el PRD no lista los campos; se tomaron los de la entidad Venue
public record VenueResponse(
        Long id,
        String code,
        String name,
        String city,
        String address,
        Integer capacity,
        Boolean active
) {
}
