package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.model.TicketStatus;
import com.pulsepass.model.TicketType;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PRD Controladores 22.5. Solo capa web, TicketService es un mock (NFR-CTRL-002, QT-CTRL-002)
@WebMvcTest(TicketController.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    // Escenario: Andrea compra una entrada GENERAL para CMF-2026 (PRD Controladores 23)
    private static final String VALID_JSON = """
            {
              "userEmail": "andrea@pulsepass.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;

    private final PurchaseTicketRequest validRequest =
            new PurchaseTicketRequest("andrea@pulsepass.com", "CMF-2026", TicketType.GENERAL);

    private TicketResponse ticket(TicketStatus status) {
        return new TicketResponse(100L, "TKT-0001", TicketType.GENERAL, new BigDecimal("150000"),
                status, LocalDateTime.of(2026, 11, 24, 10, 0),
                "andrea@pulsepass.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // --- POST /api/tickets ---

    @Test
    void purchase_compraValida_retorna201() throws Exception { // TEST-CTRL-TKT-001 / AC-CTRL-009
        when(ticketService.purchase(validRequest)).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.price").value(150000))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService).purchase(validRequest);
    }

    @Test
    void purchase_requestInvalido_retorna400SinLlamarAlService() throws Exception { // TEST-CTRL-TKT-002 / QT-CTRL-007
        String json = """
                {
                  "userEmail": "no-es-un-email",
                  "eventCode": ""
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").value("User email must be valid"))
                .andExpect(jsonPath("$.details.eventCode").value("Event code is required"))
                .andExpect(jsonPath("$.details.type").value("Ticket type is required"));

        verify(ticketService, never()).purchase(any());
    }

    @Test
    void purchase_usuarioInexistente_retorna404() throws Exception { // TEST-CTRL-TKT-003
        when(ticketService.purchase(validRequest))
                .thenThrow(new ResourceNotFoundException("User not found: andrea@pulsepass.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: andrea@pulsepass.com"));

        verify(ticketService).purchase(validRequest);
    }

    @Test
    void purchase_reglaDeNegocio_retorna409() throws Exception { // TEST-CTRL-TKT-004 / AC-CTRL-010
        // Laura tiene 17 anios y CMF-2026 exige 18: el Service rechaza, el Controller solo traduce a 409
        String json = VALID_JSON.replace("andrea@pulsepass.com", "laura@pulsepass.com");
        PurchaseTicketRequest lauraRequest =
                new PurchaseTicketRequest("laura@pulsepass.com", "CMF-2026", TicketType.GENERAL);
        when(ticketService.purchase(lauraRequest))
                .thenThrow(new BusinessRuleException("User does not meet the minimum age for event: CMF-2026"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User does not meet the minimum age for event: CMF-2026"));

        verify(ticketService).purchase(lauraRequest);
    }

    // --- Consultas ---

    @Test
    void findByCode_retorna200() throws Exception { // TEST-CTRL-TKT-005
        when(ticketService.findByCode("TKT-0001")).thenReturn(ticket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TKT-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.userEmail").value("andrea@pulsepass.com"));

        verify(ticketService).findByCode("TKT-0001");
    }

    @Test
    void findByUserEmail_retorna200() throws Exception { // TEST-CTRL-TKT-006 / UC-CTRL-08
        when(ticketService.findByUserEmail("andrea@pulsepass.com")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@pulsepass.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@pulsepass.com"));

        verify(ticketService).findByUserEmail("andrea@pulsepass.com");
    }

    @Test
    void findPaidTicketsByEvent_retorna200() throws Exception { // TEST-CTRL-TKT-007
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of(ticket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    // --- PATCH /cancel ---

    @Test
    void cancel_ticketPaid_retorna200() throws Exception { // TEST-CTRL-TKT-008 / AC-CTRL-011
        when(ticketService.cancel("TKT-0001")).thenReturn(ticket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-0001");
    }

    @Test
    void cancel_ticketUsado_retorna409() throws Exception { // TEST-CTRL-TKT-009 / AC-CTRL-012
        when(ticketService.cancel("TKT-0001"))
                .thenThrow(new BusinessRuleException("A USED ticket cannot be cancelled"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A USED ticket cannot be cancelled"));

        verify(ticketService).cancel("TKT-0001");
    }

    // --- PATCH /use ---

    @Test
    void markAsUsed_ticketPaid_retorna200() throws Exception { // TEST-CTRL-TKT-010 / AC-CTRL-013
        when(ticketService.markAsUsed("TKT-0001")).thenReturn(ticket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-0001");
    }

    @Test
    void markAsUsed_ticketCancelado_retorna409() throws Exception { // TEST-CTRL-TKT-011 / AC-CTRL-014
        when(ticketService.markAsUsed("TKT-0001"))
                .thenThrow(new BusinessRuleException("A CANCELLED ticket cannot be used"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A CANCELLED ticket cannot be used"));

        verify(ticketService).markAsUsed("TKT-0001");
    }
}
