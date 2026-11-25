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


@RestController
@RequestMapping("/api/artists")//define la ruta base
public class ArtistController {

    // Depende solo del Service (CTRL-002, NFR-CTRL-004)
    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    
    @GetMapping("/active")//indica que este método responde a peticiones GET en /api/artists/active.
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {//indicamos que devuelve una lista de objetos de artist response
        return ResponseEntity.ok(artistService.findActiveArtists());//llama al servicio
    }

    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(@RequestParam String stageName) {//requestParam toma la variable despues del bystageName
        return ResponseEntity.ok(artistService.findByStageName(stageName));
    }   

    
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable Long id) {//el valor lo toma de la ruta
        return ResponseEntity.ok(artistService.findById(id));
    }
}
