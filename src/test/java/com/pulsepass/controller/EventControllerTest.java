package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.model.EventCategory;
import com.pulsepass.model.EventStatus;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.Test;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PRD Controladores 22.2. Solo capa web, EventService es un mock (NFR-CTRL-002, QT-CTRL-002)
// Seguridad (PRD Seguridad §17.2, T-SEC-020): se importa la SecurityConfig real, sin desactivar filtros.
// Rutas públicas se prueban como anónimo; las protegidas con @WithMockUser y el rol mínimo de la matriz §11.
// La matriz completa (401/403 por rol) está en security/SeguridadRutasTest.
@WebMvcTest(EventController.class)
@Import(SecurityConfig.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // SecurityConfig la necesita para el filtro JWT y el login. Aquí no se usan tokens: basta un mock
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private EventService eventService;

    // Escenario de referencia: CMF-2026 en VEN-SMR-01 (PRD Controladores 23.1 y 23.2)
    private static final LocalDateTime EVENT_DATE = LocalDateTime.of(2026, 12, 20, 20, 0);

    private static final String VALID_JSON = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival de musica caribe en Santa Marta",
              "category": "MUSIC",
              "eventDate": "2026-12-20T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    private final CreateEventRequest validRequest = new CreateEventRequest(
            "CMF-2026", "Caribbean Music Fest 2026", "Festival de musica caribe en Santa Marta",
            EventCategory.MUSIC, EVENT_DATE, 18, "VEN-SMR-01");

    private final ArtistResponse solarBeat = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);

    private EventResponse cmf(EventStatus status, List<ArtistResponse> artists) {
        return new EventResponse(10L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de musica caribe en Santa Marta", EventCategory.MUSIC, status, EVENT_DATE,
                18, "VEN-SMR-01", "Marina Convention Center", artists);
    }

    private final EventSummaryResponse cmfSummary = new EventSummaryResponse(
            "CMF-2026", "Caribbean Music Fest 2026", EventCategory.MUSIC,
            EventStatus.PUBLISHED, EVENT_DATE, "Marina Convention Center");

    // --- POST /api/events ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_requestValido_retorna201EnDraft() throws Exception { // TEST-CTRL-EVT-001 / AC-CTRL-003
        when(eventService.create(validRequest)).thenReturn(cmf(EventStatus.DRAFT, List.of()));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.artists").isEmpty());

        verify(eventService).create(validRequest);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_requestInvalido_retorna400ConDetails() throws Exception { // TEST-CTRL-EVT-002 / AC-CTRL-004
        // Mismo ejemplo del PRD 13: faltan eventCode y venueCode
        String json = VALID_JSON
                .replace("\"eventCode\": \"CMF-2026\"", "\"eventCode\": \"\"")
                .replace("\"venueCode\": \"VEN-SMR-01\"", "\"venueCode\": \"\"");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.venueCode").value("Venue code is required"));

        verify(eventService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_edadMinimaNegativa_retorna400() throws Exception { // PRD 11.1 @Min(0)
        String json = VALID_JSON.replace("\"minimumAge\": 18", "\"minimumAge\": -1");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.minimumAge").value("Minimum age must be 0 or greater"));

        verify(eventService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_categoriaInexistente_retorna400() throws Exception { // PRD 11.4 JSON mal formado
        // "CONCIERTO" no existe en EventCategory: Jackson no puede convertirlo
        String json = VALID_JSON.replace("\"MUSIC\"", "\"CONCIERTO\"");

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(eventService, never()).create(any());
    }

    // --- GET /api/events/{eventCode} ---

    @Test
    void findByCode_existente_retorna200() throws Exception { // TEST-CTRL-EVT-003
        when(eventService.findByCode("CMF-2026")).thenReturn(cmf(EventStatus.DRAFT, List.of()));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("Caribbean Music Fest 2026"))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.minimumAge").value(18));

        verify(eventService).findByCode("CMF-2026");
    }

    @Test
    void findByCode_inexistente_retorna404() throws Exception { // TEST-CTRL-EVT-004
        when(eventService.findByCode("NO-EXISTE"))
                .thenThrow(new ResourceNotFoundException("Event not found: NO-EXISTE"));

        mockMvc.perform(get("/api/events/NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Event not found: NO-EXISTE"));

        verify(eventService).findByCode("NO-EXISTE");
    }

    // --- GET /api/events/published ---

    @Test
    void findPublishedEvents_retorna200() throws Exception { // TEST-CTRL-EVT-005
        when(eventService.findPublishedEvents()).thenReturn(List.of(cmfSummary));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    // --- PATCH /api/events/{eventCode}/publish ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void publish_eventoEnDraft_retorna200() throws Exception { // TEST-CTRL-EVT-006 / AC-CTRL-005
        when(eventService.publish("CMF-2026")).thenReturn(cmf(EventStatus.PUBLISHED, List.of()));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void publish_eventoNoDraft_retorna409() throws Exception { // TEST-CTRL-EVT-007 / AC-CTRL-006
        when(eventService.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException("Only DRAFT events can be published"));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Only DRAFT events can be published"));

        verify(eventService).publish("CMF-2026");
    }

    // --- POST /api/events/{eventCode}/artists/{artistId} ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void addArtist_asociacionValida_retorna200() throws Exception { // TEST-CTRL-EVT-008
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(cmf(EventStatus.DRAFT, List.of(solarBeat)));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addArtist_artistaRepetido_retorna409() throws Exception { // FR-CTRL-EVT-005 conflicto / BR-EVENT-010
        when(eventService.addArtist("CMF-2026", 1L))
                .thenThrow(new BusinessRuleException("Artist already associated to event: Solar Beat"));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Artist already associated to event: Solar Beat"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addArtist_artistIdNoNumerico_retorna400SinLlamarAlService() throws Exception { // QT-CTRL-007
        mockMvc.perform(post("/api/events/CMF-2026/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'artistId'"));

        verifyNoInteractions(eventService);
    }

    // --- GET /api/events/by-artist?stageName=... ---

    @Test
    void findByArtist_retorna200() throws Exception { // TEST-CTRL-EVT-009
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(cmfSummary));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }
}
