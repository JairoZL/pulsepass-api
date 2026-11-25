package com.pulsepass.integration;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRD Seguridad §17.3: aplicación completa (todas las capas reales) contra PostgreSQL en Testcontainers.
 * Registro -> hash BCrypt y ROLE_USER en la base -> login -> token -> /api/auth/me.
 * No es @Transactional: cada request hace commit de verdad, así que cada prueba usa un username propio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistroLoginIntegracionTest {

    // Contraseña ficticia, solo para pruebas (§17.3)
    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String registroJson(String username) {
        return """
                {
                  "username": "%s",
                  "email": "%s@pulsepass.com",
                  "firstName": "Andrea",
                  "lastName": "Gomez",
                  "birthDate": "2000-05-10",
                  "password": "%s"
                }
                """.formatted(username, username, PASSWORD);
    }

    private String loginJson(String identifier, String password) {
        return """
                { "identifier": "%s", "password": "%s" }
                """.formatted(identifier, password);
    }

    private long registrar(String username) throws Exception {
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson(username)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private String login(String identifier) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(identifier, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    @Test
    @DisplayName("UC-SEC-01: el registro guarda un hash BCrypt (no la contraseña) y ROLE_USER")
    void registro_guardaHashBcryptYRolUser() throws Exception {
        long id = registrar("andrea_reg");

        String hash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM user_credentials WHERE user_id = ?", String.class, id);
        assertThat(hash)
                .startsWith("$2")            // prefijo de BCrypt
                .hasSize(60)
                .isNotEqualTo(PASSWORD);

        List<String> roles = jdbcTemplate.queryForList(
                "SELECT role FROM user_roles WHERE user_id = ?", String.class, id);
        assertThat(roles).containsExactly("USER");
    }

    @Test
    @DisplayName("UC-SEC-02: login con usuario real -> el token abre /api/auth/me con el id persistente")
    void login_tokenAbreMe() throws Exception {
        long id = registrar("andrea_login");
        String token = login("andrea_login");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.username").value("andrea_login"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @DisplayName("FR-SEC-AUTH-002: el login también acepta el correo, sin importar mayúsculas")
    void login_porCorreo() throws Exception {
        registrar("andrea_mail");

        String token = login("ANDREA_MAIL@pulsepass.com");

        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("FR-SEC-AUTH-003: contraseña incorrecta y usuario inexistente -> el mismo 401")
    void login_fallido_mismoMensaje() throws Exception {
        registrar("andrea_mala");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("andrea_mala", "ContrasenaEquivocada1")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nadie_existe", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("UC-SEC-10 / demo §23 paso 10: se desactiva al usuario con token vigente -> 401 en el siguiente request")
    void usuarioDesactivado_pierdeAccesoConTokenVigente() throws Exception {
        long id = registrar("andrea_off");
        String token = login("andrea_off");

        jdbcTemplate.update("UPDATE users SET active = FALSE WHERE id = ?", id);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("andrea_off", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registro duplicado -> 409 y el usuario original conserva una sola credencial")
    void registroDuplicado_noCreaOtraCredencial() throws Exception {
        long id = registrar("andrea_dup");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(registroJson("andrea_dup")))
                .andExpect(status().isConflict());

        Integer credenciales = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_credentials WHERE user_id = ?", Integer.class, id);
        assertThat(credenciales).isEqualTo(1);
    }
}
