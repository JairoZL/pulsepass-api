package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Contrato HTTP: PRD Controladores 8.5 (FR-CTRL-TKT-001 .. FR-CTRL-TKT-006)
// Sin @RequestMapping a nivel de clase: un endpoint vive bajo /api/events (tickets pagados de un evento)
// y el resto bajo /api/tickets. Asi todo lo de tickets depende solo de TicketService (NFR-CTRL-004)
@RestController
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // FR-CTRL-TKT-001. Compra -> 201. El precio no viaja en el request: lo calcula el Service.
    // Usuario inactivo, menor de edad, evento no publicado o sin cupo -> BusinessRuleException (409)
    @PostMapping("/api/tickets")
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request) {
        TicketResponse created = ticketService.purchase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // FR-CTRL-TKT-003. Ruta literal: tiene prioridad sobre /{ticketCode}
    @GetMapping("/api/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email) {
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }

    // FR-CTRL-TKT-002
    @GetMapping("/api/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    // FR-CTRL-TKT-004. Solo tickets en estado PAID del evento
    @GetMapping("/api/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }

    // FR-CTRL-TKT-005. PAID -> CANCELLED. Si el ticket no esta PAID (ej. USED) -> 409
    @PatchMapping("/api/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    // FR-CTRL-TKT-006. PAID -> USED. Si el ticket esta CANCELLED -> 409
    @PatchMapping("/api/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
