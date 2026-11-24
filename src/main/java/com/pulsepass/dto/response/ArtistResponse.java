package com.pulsepass.dto.response;

// POR ACORDAR: el PRD no lista los campos; se tomaron los de la entidad Artist
public record ArtistResponse(
        Long id,
        String stageName,
        String country,
        String genre,
        Boolean active
) {
}
