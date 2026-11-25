package com.pulsepass.controller;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Contrato HTTP: PRD Controladores 8.2 (FR-CTRL-EVT-001 .. FR-CTRL-EVT-006)
// GET /api/events/{eventCode}/tickets/paid tambien empieza por /api/events, pero lo atiende
// TicketController: asi este Controller depende solo de EventService (NFR-CTRL-004)
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    // FR-CTRL-EVT-001. Crear responde 201. El request no trae status: el Service lo pone en DRAFT (BR-EVENT-005)
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse created = eventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // FR-CTRL-EVT-003. Ruta literal: tiene prioridad sobre /{eventCode}
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    // FR-CTRL-EVT-006. /api/events/by-artist?stageName=...
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName) {
        return ResponseEntity.ok(eventService.findByArtist(stageName));
    }

    // FR-CTRL-EVT-002
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.findByCode(eventCode));
    }

    // FR-CTRL-EVT-004. PATCH: cambia solo una parte del recurso (su estado DRAFT -> PUBLISHED).
    // Si el evento no esta en DRAFT, el Service lanza BusinessRuleException (409)
    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    // FR-CTRL-EVT-005. Responde 200 (no 201): no se crea un recurso nuevo, se actualiza el evento.
    // Artista repetido o evento CANCELLED/FINISHED -> BusinessRuleException (409)
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(@PathVariable String eventCode,
                                                   @PathVariable Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }
}
