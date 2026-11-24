package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PRD Controladores §22.1. Solo levanta la capa web: sin PostgreSQL ni Services reales (NFR-CTRL-002)
@WebMvcTest(VenueController.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock del Service (QT-CTRL-002)
    @MockitoBean
    private VenueService venueService;

    // Datos de referencia del escenario: PRD Controladores §23.1
    private final VenueResponse marina = new VenueResponse(
            1L, "VEN-SMR-01", "Marina Convention Center", "Santa Marta", "Calle 1 # 2-3", 3, true);

    @Test
    void findByCode_venueExistente_retorna200() throws Exception { // TEST-CTRL-VEN-001 / AC-CTRL-001
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(marina);

        mockMvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"))
                .andExpect(jsonPath("$.capacity").value(3));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    @Test
    void findByCode_venueInexistente_retorna404ConErrorResponse() throws Exception { // TEST-CTRL-VEN-002 / AC-CTRL-002
        when(venueService.findByCode("NO-EXISTE"))
                .thenThrow(new ResourceNotFoundException("Venue not found: NO-EXISTE"));

        mockMvc.perform(get("/api/venues/NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Venue not found: NO-EXISTE"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());

        verify(venueService).findByCode("NO-EXISTE");
    }

    @Test
    void findActiveVenues_retorna200ConLista() throws Exception { // TEST-CTRL-VEN-003
        when(venueService.findActiveVenues()).thenReturn(List.of(marina));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(venueService).findActiveVenues();
    }
}
