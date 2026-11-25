package com.pulsepass.mapper;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.model.Venue;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {

    VenueResponse toResponse(Venue venue);
}
