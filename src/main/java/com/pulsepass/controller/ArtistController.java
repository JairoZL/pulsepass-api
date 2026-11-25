package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.service.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Contrato HTTP: PRD Controladores 8.3 (FR-CTRL-ART-001, FR-CTRL-ART-002, FR-CTRL-ART-003)
@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    // Depende solo del Service (CTRL-002, NFR-CTRL-004)
    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    // FR-CTRL-ART-003. Ruta literal: tiene prioridad sobre /{id}
    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {
        return ResponseEntity.ok(artistService.findActiveArtists());
    }

    // FR-CTRL-ART-002. stageName viaja como query param: /api/artists/by-stage-name?stageName=Solar Beat
    // @RequestParam es obligatorio por defecto: si falta, Spring lanza MissingServletRequestParameterException (400)
    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(@RequestParam String stageName) {
        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }

    // FR-CTRL-ART-001. Spring convierte el texto de la URL a Long; si no puede (ej. "abc"),
    // lanza MethodArgumentTypeMismatchException (400) antes de entrar al metodo
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(artistService.findById(id));
    }
}
