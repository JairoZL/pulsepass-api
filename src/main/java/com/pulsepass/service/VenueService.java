package com.pulsepass.service;

import com.pulsepass.dto.response.VenueResponse;

import java.util.List;

// Contrato: PRD Servicios §10
public interface VenueService {

    VenueResponse findByCode(String code);

    List<VenueResponse> findActiveVenues();
}
