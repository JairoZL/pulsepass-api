package com.pulsepass.integration;

import com.pulsepass.TestcontainersConfiguration;
import com.pulsepass.model.UserRole;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRD Seguridad NFR-SEC-004 y DoD §22: el registro es atómico.
 * Se simula una falla en el ÚLTIMO paso (guardar ROLE_USER) y se comprueba en PostgreSQL real
 * que no quedó nada a medias: ni usuario, ni perfil, ni credencial.
 * Clase aparte porque @MockitoBean cambia el contexto de Spring para todas sus pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class RegistroAtomicoIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Reemplaza el repositorio real de roles por un mock que falla al guardar
    @MockitoBean
    private UserRoleRepository userRoleRepository;

    @Test
    @DisplayName("NFR-SEC-004: si falla guardar el rol, se revierten usuario, perfil y credencial")
    void fallaAlGuardarElRol_noQuedaNadaGuardado() throws Exception {
        when(userRoleRepository.save(any(UserRole.class)))
                .thenThrow(new RuntimeException("Fallo simulado al guardar el rol"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
                        {
                          "username": "atomico",
                          "email": "atomico@pulsepass.com",
                          "firstName": "Andrea",
                          "lastName": "Gomez",
                          "birthDate": "2000-05-10",
                          "password": "UnaFraseSeguraLarga2026!"
                        }
                        """))
                .andExpect(status().isInternalServerError());

        Integer usuarios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE username = 'atomico'", Integer.class);
        Integer perfiles = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_profiles p JOIN users u ON u.id = p.user_id "
                        + "WHERE u.username = 'atomico'", Integer.class);
        Integer credenciales = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_credentials", Integer.class);

        assertThat(usuarios).isZero();
        assertThat(perfiles).isZero();
        assertThat(credenciales).isZero();
    }
}
