package com.pulsepass.security;

import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRD Seguridad §11 (matriz de autorización), §17.2 y §17.4: para cada ruta se prueba
 * anónimo, rol prohibido y rol permitido.
 * Se cargan TODOS los controladores con la SecurityConfig real y los filtros activos
 * (§17.2: no se usa addFilters = false). Los Services son mocks: aquí solo importa
 * si la solicitud pasa o no el filtro de seguridad, no el resultado de negocio.
 * Propiedad (ticket ajeno) y visibilidad de borradores se prueban en la etapa 5.
 */
@WebMvcTest
@Import(SecurityConfig.class)
class SeguridadRutasTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private VenueService venueService;
    @MockitoBean private EventService eventService;
    @MockitoBean private ArtistService artistService;
    @MockitoBean private UserService userService;
    @MockitoBean private TicketService ticketService;
    @MockitoBean private AuthService authService;
    // SecurityConfig la necesita para el filtro JWT; aquí la identidad se simula con user(...)
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    // Los POST llevan "{}": el cuerpo no importa para seguridad (el filtro decide antes de leerlo)
    private MockHttpServletRequestBuilder solicitud(String metodo, String url) {
        return request(HttpMethod.valueOf(metodo), url)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}");
    }

    private void verificarQueNingunServiceFueLlamado() {
        verifyNoInteractions(venueService, eventService, artistService, userService, ticketService, authService);
    }

    // ---------- Rutas públicas: un anónimo NO recibe 401 ni 403 ----------

    @ParameterizedTest(name = "[{index}] anónimo {0} {1} -> sin 401/403")
    @CsvSource({
            "GET,  /api/venues/VEN-SMR-01",                       // FR-SEC-VEN-01
            "GET,  /api/venues/active",                           // FR-SEC-VEN-02 (§14.3 adv. 3)
            "GET,  /api/artists/1",                               // FR-SEC-ART-01
            "GET,  /api/artists/by-stage-name?stageName=Solar",   // FR-SEC-ART-02
            "GET,  /api/artists/active",                          // FR-SEC-ART-03
            "GET,  /api/events/published",                        // FR-SEC-EVT-03 / T-SEC-001
            "GET,  /api/events/CMF-2026",                         // FR-SEC-EVT-02 (borradores: etapa 5)
            "GET,  /api/events/by-artist?stageName=Solar",        // FR-SEC-EVT-06 (borradores: etapa 5)
            "POST, /api/users",                                   // FR-SEC-USR-01 (además prueba que no hay CSRF)
            "POST, /api/auth/login",                              // §10.1
    })
    void rutaPublica_anonimo_noRecibe401Ni403(String metodo, String url) throws Exception {
        // Dado: sin identidad. Cuando: se llama la ruta pública
        MvcResult resultado = mockMvc.perform(solicitud(metodo, url)).andReturn();

        // Entonces: la seguridad la deja pasar (el código final lo decide el controlador: 200, 400...)
        assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ---------- Rutas protegidas: un anónimo recibe 401 con ErrorResponse ----------

    @ParameterizedTest(name = "[{index}] anónimo {0} {1} -> 401")
    @CsvSource({
            "POST,  /api/events",                                 // FR-SEC-EVT-01 / T-SEC-002
            "PATCH, /api/events/CMF-2026/publish",                // FR-SEC-EVT-04
            "POST,  /api/events/CMF-2026/artists/1",              // FR-SEC-EVT-05
            "GET,   /api/users/by-email?email=andrea@pulsepass.com", // FR-SEC-USR-02
            "GET,   /api/users/by-username?username=andrea",      // FR-SEC-USR-03
            "POST,  /api/tickets",                                // FR-SEC-TKT-01
            "GET,   /api/tickets/TKT-0001",                       // FR-SEC-TKT-02
            "GET,   /api/tickets/by-user?email=andrea@pulsepass.com", // FR-SEC-TKT-03
            "GET,   /api/events/CMF-2026/tickets/paid",           // FR-SEC-TKT-04
            "PATCH, /api/tickets/TKT-0001/cancel",                // FR-SEC-TKT-05
            "PATCH, /api/tickets/TKT-0001/use",                   // FR-SEC-TKT-06
            "GET,   /api/auth/me",                                // FR-SEC-AUTH-010
            "GET,   /api/no-existe",                              // SEC-001: denegar por defecto
    })
    void rutaProtegida_anonimo_retorna401SinLlamarAlService(String metodo, String url) throws Exception {
        mockMvc.perform(solicitud(metodo, url))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.MESSAGE))
                .andExpect(jsonPath("$.details").isEmpty());

        verificarQueNingunServiceFueLlamado();
    }

    // ---------- Rol insuficiente: 403 y el Service nunca se ejecuta ----------

    @ParameterizedTest(name = "[{index}] {2} {0} {1} -> 403")
    @CsvSource({
            "POST,  /api/events,                       USER",   // T-SEC-003
            "POST,  /api/events,                       STAFF",
            "PATCH, /api/events/CMF-2026/publish,      USER",   // UC-SEC-08
            "PATCH, /api/events/CMF-2026/publish,      STAFF",
            "POST,  /api/events/CMF-2026/artists/1,    USER",
            "POST,  /api/events/CMF-2026/artists/1,    STAFF",
            "GET,   /api/events/CMF-2026/tickets/paid, USER",
            "PATCH, /api/tickets/TKT-0001/use,         USER",   // T-SEC-012 / UC-SEC-07
            "POST,  /api/tickets,                      STAFF",  // TKT-01: comprar es de USER
            "POST,  /api/tickets,                      ADMIN",
    })
    void rolInsuficiente_retorna403SinLlamarAlService(String metodo, String url, String rol) throws Exception {
        mockMvc.perform(solicitud(metodo, url).with(user("tester").roles(rol)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value(RestAccessDeniedHandler.MESSAGE))
                .andExpect(jsonPath("$.details").isEmpty());

        verificarQueNingunServiceFueLlamado();
    }

    // ---------- Rol permitido: la seguridad deja pasar ----------

    @ParameterizedTest(name = "[{index}] {2} {0} {1} -> sin 401/403")
    @CsvSource({
            "POST,  /api/events,                                  ADMIN", // T-SEC-004
            "PATCH, /api/events/CMF-2026/publish,                 ADMIN", // T-SEC-014
            "POST,  /api/events/CMF-2026/artists/1,               ADMIN",
            "GET,   /api/events/CMF-2026/tickets/paid,            STAFF",
            "GET,   /api/events/CMF-2026/tickets/paid,            ADMIN",
            "PATCH, /api/tickets/TKT-0001/use,                    STAFF", // T-SEC-013
            "PATCH, /api/tickets/TKT-0001/use,                    ADMIN",
            "POST,  /api/tickets,                                 USER",
            "GET,   /api/tickets/TKT-0001,                        USER",  // propiedad: etapa 5
            "GET,   /api/tickets/by-user?email=andrea@pulsepass.com, USER",
            "PATCH, /api/tickets/TKT-0001/cancel,                 USER",
            "GET,   /api/users/by-email?email=andrea@pulsepass.com, USER",
            "GET,   /api/users/by-username?username=andrea,       USER",
            "GET,   /api/auth/me,                                 USER",
    })
    void rolPermitido_noRecibe401Ni403(String metodo, String url, String rol) throws Exception {
        MvcResult resultado = mockMvc.perform(solicitud(metodo, url).with(user("tester").roles(rol)))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isNotIn(401, 403);
    }

    // ---------- Stateless (FR-SEC-AUTH-008) ----------

    @Test
    void solicitudAutenticada_noCreaSesionHttp() throws Exception {
        // Dado: un usuario autenticado. Cuando: hace una solicitud
        MvcResult resultado = mockMvc.perform(get("/api/users/by-username").param("username", "andrea")
                        .with(user("andrea").roles("USER")))
                .andReturn();

        // Entonces: el servidor no guardó sesión (no hay JSESSIONID que robar)
        assertThat(resultado.getRequest().getSession(false)).isNull();
    }

    // ---------- AccessDeniedException lanzada dentro del Service (SEC-009) ----------
    // Prepara la etapa 5: cuando @PreAuthorize niegue el acceso dentro del Service,
    // GlobalExceptionHandler no debe convertirlo en 500.

    @Test
    void accessDeniedDesdeElService_usuarioAutenticado_retorna403NoUn500() throws Exception {
        when(venueService.findActiveVenues()).thenThrow(new AccessDeniedException("denegado"));

        mockMvc.perform(get("/api/venues/active").with(user("andrea").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(RestAccessDeniedHandler.MESSAGE));
    }

    @Test
    void accessDeniedDesdeElService_anonimo_retorna401NoUn500() throws Exception {
        when(venueService.findActiveVenues()).thenThrow(new AccessDeniedException("denegado"));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.MESSAGE));
    }
}
