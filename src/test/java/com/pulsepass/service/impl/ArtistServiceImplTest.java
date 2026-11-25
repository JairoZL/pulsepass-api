package com.pulsepass.service.impl;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.ArtistMapper;
import com.pulsepass.model.Artist;
import com.pulsepass.repository.ArtistRepository;
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
class ArtistServiceImplTest {

    private static final Long ID = 10L;
    private static final String STAGE_NAME = "Solar Beat";

    @Mock private ArtistRepository artistRepository;
    @Mock private ArtistMapper artistMapper;

    @InjectMocks
    private ArtistServiceImpl artistService;

    @Test
    @DisplayName("FR-SVC-009: findById existente -> retorna DTO")
    void findById_existingArtist_returnsDto() {
        // ARRANGE
        Artist artist = artist();
        ArtistResponse expected = response();
        when(artistRepository.findById(ID)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        ArtistResponse result = artistService.findById(ID);

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("BR-ARTIST-001: findById inexistente -> ResourceNotFoundException")
    void findById_missingArtist_throwsNotFound() {
        // ARRANGE
        when(artistRepository.findById(ID)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> artistService.findById(ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: " + ID);
    }

    @Test
    @DisplayName("FR-SVC-009: findByStageName existente -> retorna DTO")
    void findByStageName_existingArtist_returnsDto() {
        // ARRANGE
        Artist artist = artist();
        ArtistResponse expected = response();
        when(artistRepository.findByStageNameIgnoreCase(STAGE_NAME)).thenReturn(Optional.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        ArtistResponse result = artistService.findByStageName(STAGE_NAME);

        // ASSERT
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("BR-ARTIST-001: findByStageName inexistente -> ResourceNotFoundException")
    void findByStageName_missingArtist_throwsNotFound() {
        // ARRANGE
        when(artistRepository.findByStageNameIgnoreCase(STAGE_NAME)).thenReturn(Optional.empty());

        // ACT + ASSERT
        assertThatThrownBy(() -> artistService.findByStageName(STAGE_NAME))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Artist not found: " + STAGE_NAME);
    }

    @Test
    @DisplayName("BR-ARTIST-002: findActiveArtists usa la consulta de activos y mapea cada artista")
    void findActiveArtists_returnsOnlyActiveArtistsMapped() {
        // ARRANGE
        Artist artist = artist();
        ArtistResponse expected = response();
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of(artist));
        when(artistMapper.toResponse(artist)).thenReturn(expected);

        // ACT
        List<ArtistResponse> result = artistService.findActiveArtists();

        // ASSERT
        assertThat(result).containsExactly(expected);
        // Nunca se trae todo con findAll() para filtrar en Java
        verify(artistRepository, never()).findAll();
    }

    @Test
    @DisplayName("BR-ARTIST-002: sin artistas activos -> lista vacía")
    void findActiveArtists_noActiveArtists_returnsEmptyList() {
        // ARRANGE
        when(artistRepository.findByActiveTrueOrderByStageNameAsc()).thenReturn(List.of());

        // ACT
        List<ArtistResponse> result = artistService.findActiveArtists();

        // ASSERT
        assertThat(result).isEmpty();
    }

    private Artist artist() {
        return new Artist(STAGE_NAME, "Colombia", "Pop");
    }

    private ArtistResponse response() {
        return new ArtistResponse(ID, STAGE_NAME, "Colombia", "Pop", true);
    }
}
