package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ArtistController.class)//simula el get de un cliente
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;//simulará una version del user sevice

    @MockitoBean
    private ArtistService artistService;

    
    private final ArtistResponse solarBeat = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);
    private final ArtistResponse neonWaves = new ArtistResponse(2L, "Neon Waves", "Mexico", "Synthwave", true);

    @Test
    void findById_artistaExistente_retorna200() throws Exception { 
        when(artistService.findById(1L)).thenReturn(solarBeat);

        mockMvc.perform(get("/api/artists/1"))//enviamos la peticion al controlador y simulamos el get con los andExpect
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.genre").value("Electronic"));

        verify(artistService).findById(1L);
    }

    @Test
    void findById_artistaInexistente_retorna404() throws Exception { // TEST-CTRL-ART-002
        when(artistService.findById(99L)).thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Artist not found: 99"));

        verify(artistService).findById(99L);
    }

    @Test
    void findByStageName_retorna200() throws Exception { // TEST-CTRL-ART-003
        when(artistService.findByStageName("Solar Beat")).thenReturn(solarBeat);

        // .param() arma el query string y codifica el espacio de "Solar Beat"
        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.country").value("Colombia"));

        verify(artistService).findByStageName("Solar Beat");
    }

    @Test
    void findActiveArtists_retorna200ConLista() throws Exception { // TEST-CTRL-ART-004
        when(artistService.findActiveArtists()).thenReturn(List.of(solarBeat, neonWaves));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"));

        verify(artistService).findActiveArtists();
    }

    // --- Casos extra: errores de la capa HTTP que nunca deben llegar al Service ---

    @Test
    void findById_idNoNumerico_retorna400SinLlamarAlService() throws Exception { // QT-CTRL-007
        mockMvc.perform(get("/api/artists/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));

        verifyNoInteractions(artistService);
    }

    @Test
    void findByStageName_sinParametro_retorna400SinLlamarAlService() throws Exception { // QT-CTRL-007
        mockMvc.perform(get("/api/artists/by-stage-name"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Missing required parameter 'stageName'"));

        verifyNoInteractions(artistService);
    }

    @Test
    void rutaInexistente_retorna404ConErrorResponse() throws Exception { // NFR-CTRL-003
        mockMvc.perform(get("/api/artistas"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}
