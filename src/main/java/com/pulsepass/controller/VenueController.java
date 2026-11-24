package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController// convierte la response de un objeto java a json
@RequestMapping("/api/venues")
public class VenueController {

    // Depende solo del Service, nunca del Repository (CTRL-002, NFR-CTRL-004)
    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    
    //Define una ruta que recibe solicitudes GET
    @GetMapping("/active")//define el endpoint para consultar los venues activos.
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        return ResponseEntity.ok(venueService.findActiveVenues());
    }

   
    @GetMapping("/{code}")//define el endpoint para consultar un venue específico usando el código que viene en la URL.
    public ResponseEntity<VenueResponse> findByCode(@PathVariable String code) {
        return ResponseEntity.ok(venueService.findByCode(code));
    }
}
