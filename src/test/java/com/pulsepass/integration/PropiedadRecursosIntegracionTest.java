package com.pulsepass.integration;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRD Seguridad §11.1 (anti-IDOR) y FR-SEC-EVT-02/06, contra PostgreSQL real (Testcontainers).
 * Escenario de la demo del §23:
 *   - andrea y carlos: USER, cada uno con 2 tickets del evento publicado SEC-PUB
 *   - laura: USER + STAFF;  admin: USER + ADMIN
 *   - SEC-DRAFT: evento en borrador. Ambos eventos tienen al artista "Solar Beat" (V2)
 * Todo pasa por HTTP con tokens reales: filtro JWT, @PreAuthorize/@PostAuthorize y Services reales.
 * PER_CLASS permite que @BeforeAll no sea static y use mockMvc para armar los datos una sola vez.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropiedadRecursosIntegracionTest {

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";
    private static final String MENSAJE_403 = "No tiene permisos para realizar esta operación";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String tokenAndrea;
    private String tokenCarlos;
    private String tokenLaura;
    private String tokenAdmin;

    private String ticketAndrea;          // se lee en varias pruebas, nunca se cancela
    private String ticketAndreaACancelar; // solo para "cancelar ticket propio"
    private String ticketCarlos;          // objetivo de los intentos de Andrea; nunca se cancela
    private String ticketCarlosAdmin;     // solo para "ADMIN cancela ticket ajeno"

    // ---------- Datos de prueba ----------

    @BeforeAll
    void prepararEscenario() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO venues (code, name, city, address, capacity)
                VALUES ('VEN-SEC', 'Arena Seguridad', 'Santa Marta', 'Calle 1', 100)""");
        Timestamp enUnMes = Timestamp.valueOf(LocalDateTime.now().plusDays(30));
        for (String[] evento : new String[][]{{"SEC-PUB", "PUBLISHED"}, {"SEC-DRAFT", "DRAFT"}}) {
            jdbcTemplate.update("""
                    INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
                    SELECT ?, ?, 'Evento de prueba', 'MUSIC', ?, ?, 0, id FROM venues WHERE code = 'VEN-SEC'""",
                    evento[0], "Evento " + evento[0], evento[1], enUnMes);
        }
        jdbcTemplate.update("""
                INSERT INTO event_artists (event_id, artist_id)
                SELECT e.id, a.id FROM events e, artists a
                WHERE e.event_code IN ('SEC-PUB', 'SEC-DRAFT') AND a.stage_name = 'Solar Beat'""");

        for (String username : new String[]{"andrea_p", "carlos_p", "laura_p", "admin_p"}) {
            registrar(username);
        }
        // Los roles privilegiados no salen del registro público (NFR-SEC-009): se asignan en la base
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) SELECT id, 'STAFF' FROM users WHERE username = 'laura_p'");
        jdbcTemplate.update("INSERT INTO user_roles (user_id, role) SELECT id, 'ADMIN' FROM users WHERE username = 'admin_p'");

        tokenAndrea = login("andrea_p");
        tokenCarlos = login("carlos_p");
        tokenLaura = login("laura_p");
        tokenAdmin = login("admin_p");

        ticketAndrea = comprar(tokenAndrea, "andrea_p@pulsepass.com");
        ticketAndreaACancelar = comprar(tokenAndrea, "andrea_p@pulsepass.com");
        ticketCarlos = comprar(tokenCarlos, "carlos_p@pulsepass.com");
        ticketCarlosAdmin = comprar(tokenCarlos, "carlos_p@pulsepass.com");
    }

    private void registrar(String username) throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        { "username": "%s", "email": "%s@pulsepass.com", "firstName": "Prueba",
                          "lastName": "Seguridad", "birthDate": "2000-05-10", "password": "%s" }
                        """.formatted(username, username, PASSWORD)))
                .andExpect(status().isCreated());
    }

    private String login(String username) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"identifier\": \"%s\", \"password\": \"%s\" }".formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private MockHttpServletRequestBuilder compra(String token, String userEmail) {
        return post("/api/tickets").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"userEmail\": \"%s\", \"eventCode\": \"SEC-PUB\", \"type\": \"GENERAL\" }"
                        .formatted(userEmail));
    }

    private String comprar(String token, String userEmail) throws Exception {
        String body = mockMvc.perform(compra(token, userEmail))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.ticketCode");
    }

    private ResultActions como(String token, MockHttpServletRequestBuilder request) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return mockMvc.perform(request);
    }

    private void esperar403(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(MENSAJE_403));
    }

    private String estadoDelTicket(String ticketCode) {
        return jdbcTemplate.queryForObject("SELECT status FROM tickets WHERE ticket_code = ?", String.class, ticketCode);
    }

    // ---------- Tickets: FR-SEC-TKT-01..05 ----------

    @Test
    @DisplayName("FR-SEC-TKT-02: Andrea consulta su propio ticket -> 200")
    void ticketPropio_200() throws Exception {
        como(tokenAndrea, get("/api/tickets/" + ticketAndrea))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value(ticketAndrea));
    }

    @Test
    @DisplayName("T-SEC-009: Andrea consulta el ticket de Carlos conociendo el código -> 403")
    void ticketAjeno_403() throws Exception {
        esperar403(como(tokenAndrea, get("/api/tickets/" + ticketCarlos)));
    }

    @Test
    @DisplayName("§11.1: un USER recibe 403 también con un código inexistente (no puede adivinar códigos)")
    void ticketInexistente_userRecibe403() throws Exception {
        esperar403(como(tokenAndrea, get("/api/tickets/TKT-NO-EXISTE")));
    }

    @Test
    @DisplayName("ADMIN con código inexistente -> 404 normal del Service")
    void ticketInexistente_adminRecibe404() throws Exception {
        como(tokenAdmin, get("/api/tickets/TKT-NO-EXISTE")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR-SEC-TKT-02: STAFF y ADMIN consultan cualquier ticket -> 200")
    void ticketAjeno_staffYAdmin_200() throws Exception {
        como(tokenLaura, get("/api/tickets/" + ticketCarlos)).andExpect(status().isOk());
        como(tokenAdmin, get("/api/tickets/" + ticketCarlos)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("T-SEC-010: Andrea compra con el correo de Carlos -> 403 y 0 tickets creados")
    void compraConCorreoAjeno_403SinCrearTicket() throws Exception {
        String sql = "SELECT COUNT(*) FROM tickets t JOIN users u ON u.id = t.user_id WHERE u.username = 'carlos_p'";
        Integer antes = jdbcTemplate.queryForObject(sql, Integer.class);

        esperar403(mockMvc.perform(compra(tokenAndrea, "carlos_p@pulsepass.com")));

        assertThat(jdbcTemplate.queryForObject(sql, Integer.class)).isEqualTo(antes);
    }

    @Test
    @DisplayName("T-SEC-011: Andrea cancela el ticket de Carlos -> 403 y el ticket no cambia")
    void cancelarTicketAjeno_403SinCambiarEstado() throws Exception {
        String antes = estadoDelTicket(ticketCarlos);

        esperar403(como(tokenAndrea, patch("/api/tickets/" + ticketCarlos + "/cancel")));

        assertThat(estadoDelTicket(ticketCarlos)).isEqualTo(antes);
    }

    @Test
    @DisplayName("T-SEC-011: Andrea cancela su propio ticket -> 200 (reglas del Service intactas)")
    void cancelarTicketPropio_200() throws Exception {
        como(tokenAndrea, patch("/api/tickets/" + ticketAndreaACancelar + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("FR-SEC-TKT-05: ADMIN cancela un ticket ajeno -> 200")
    void cancelarTicketAjeno_admin_200() throws Exception {
        como(tokenAdmin, patch("/api/tickets/" + ticketCarlosAdmin + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("FR-SEC-TKT-05: STAFF no cancela tickets ajenos -> 403")
    void cancelarTicketAjeno_staff_403() throws Exception {
        esperar403(como(tokenLaura, patch("/api/tickets/" + ticketCarlos + "/cancel")));
    }

    @Test
    @DisplayName("FR-SEC-TKT-03: tickets por correo -> propio 200, ajeno 403, ADMIN 200, STAFF 403")
    void ticketsPorCorreo() throws Exception {
        como(tokenAndrea, get("/api/tickets/by-user").param("email", "andrea_p@pulsepass.com"))
                .andExpect(status().isOk());
        esperar403(como(tokenAndrea, get("/api/tickets/by-user").param("email", "carlos_p@pulsepass.com")));
        como(tokenAdmin, get("/api/tickets/by-user").param("email", "carlos_p@pulsepass.com"))
                .andExpect(status().isOk());
        esperar403(como(tokenLaura, get("/api/tickets/by-user").param("email", "carlos_p@pulsepass.com")));
    }

    // ---------- Usuarios: FR-SEC-USR-02/03 ----------

    @Test
    @DisplayName("T-SEC-015: Andrea busca los datos de Carlos por correo o username -> 403")
    void datosDeOtroUsuario_403() throws Exception {
        esperar403(como(tokenAndrea, get("/api/users/by-email").param("email", "carlos_p@pulsepass.com")));
        esperar403(como(tokenAndrea, get("/api/users/by-username").param("username", "carlos_p")));
    }

    @Test
    @DisplayName("FR-SEC-USR-02/03: sus propios datos -> 200; ADMIN ve los de cualquiera -> 200")
    void datosPropiosYAdmin_200() throws Exception {
        como(tokenAndrea, get("/api/users/by-email").param("email", "ANDREA_P@pulsepass.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("andrea_p"));
        como(tokenAndrea, get("/api/users/by-username").param("username", "andrea_p"))
                .andExpect(status().isOk());
        como(tokenAdmin, get("/api/users/by-username").param("username", "carlos_p"))
                .andExpect(status().isOk());
    }

    // ---------- Eventos: FR-SEC-EVT-02/06 ----------

    @Test
    @DisplayName("T-SEC-017: un borrador da al público el mismo 404 que un código inexistente")
    void borrador_publicoRecibeElMismo404QueUnInexistente() throws Exception {
        mockMvc.perform(get("/api/events/SEC-DRAFT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: SEC-DRAFT"))
                .andExpect(jsonPath("$.name").doesNotExist());
        mockMvc.perform(get("/api/events/NO-EXISTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: NO-EXISTE"));
    }

    @Test
    @DisplayName("FR-SEC-EVT-02: un USER tampoco ve borradores -> 404")
    void borrador_user_404() throws Exception {
        como(tokenAndrea, get("/api/events/SEC-DRAFT")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR-SEC-EVT-02: STAFF y ADMIN sí ven el borrador; el publicado es público")
    void borrador_staffYAdmin_200_publicadoPublico() throws Exception {
        como(tokenLaura, get("/api/events/SEC-DRAFT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        como(tokenAdmin, get("/api/events/SEC-DRAFT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(get("/api/events/SEC-PUB"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("FR-SEC-EVT-06: por artista, el público solo ve publicados; ADMIN ve ambos")
    void eventosPorArtista_filtradosParaElPublico() throws Exception {
        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventCode").value(hasItem("SEC-PUB")))
                .andExpect(jsonPath("$[*].eventCode").value(not(hasItem("SEC-DRAFT"))));
        como(tokenAdmin, get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventCode").value(hasItems("SEC-PUB", "SEC-DRAFT")));
    }
}
