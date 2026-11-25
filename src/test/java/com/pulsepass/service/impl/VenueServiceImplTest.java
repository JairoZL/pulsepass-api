package com.pulsepass.service.impl;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.VenueMapper;
import com.pulsepass.model.Venue;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Servicios §42): Service real + Repository/Mapper mock.
@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    private static final String CODE = "VEN-SMR-01";

    @Mock private VenueRepository venueRepository;
    @Mock private VenueMapper venueMapper;

    @InjectMocks
    private VenueServiceImpl venueService;

    @Test
    @DisplayName("FR-SVC-001: venue existente -> retorna DTO")
    void findByCode_existingVenue_returnsDto() {
        // ARRANGE
        Venue venue = venue();
        VenueResponse expected = response();
        when(venueRepository.findByCode(CODE)).thenReturn(Optional.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(expected);

        // ACT
        VenueResponse result = venueService.findByCode(CODE);

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("BR-VENUE-001: venue inexistente -> ResourceNotFoundException")
    void findByCode_missingVenue_throwsNotFound() {
        // ARRANGE
        when(venueRepository.findByCode(CODE)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> venueService.findByCode(CODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Venue not found: " + CODE);
        verify(venueMapper, never()).toResponse(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("BR-VENUE-002: findActiveVenues usa la consulta de activos y mapea cada venue")
    void findActiveVenues_returnsOnlyActiveVenuesMapped() {
        // ARRANGE
        Venue venue = venue();
        VenueResponse expected = response();
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(venue));
        when(venueMapper.toResponse(venue)).thenReturn(expected);

        // ACT
        List<VenueResponse> result = venueService.findActiveVenues();

        // ASSERT
        assertThat(result).containsExactly(expected);
        // Nunca se trae todo con findAll() para filtrar en Java
        verify(venueRepository, never()).findAll();
    }

    @Test
    @DisplayName("BR-VENUE-002: sin venues activos -> lista vacía")
    void findActiveVenues_noActiveVenues_returnsEmptyList() {
        // ARRANGE
        when(venueRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of());

        // ACT
        List<VenueResponse> result = venueService.findActiveVenues();

        // ASSERT
        assertThat(result).isEmpty();
    }

    private Venue venue() {
        return new Venue(CODE, "Marina Convention Center", "Santa Marta", "Calle 1", 3);
    }

    private VenueResponse response() {
        return new VenueResponse(1L, CODE, "Marina Convention Center", "Santa Marta", "Calle 1", 3, true);
    }
}
