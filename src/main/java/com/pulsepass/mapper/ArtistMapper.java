package com.pulsepass.mapper;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.model.Artist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtistMapper {

    ArtistResponse toResponse(Artist artist);
}
