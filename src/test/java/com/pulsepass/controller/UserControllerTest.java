package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PRD Controladores 22.4. Solo capa web, UserService es un mock (NFR-CTRL-002, QT-CTRL-002)
// Seguridad (PRD Seguridad §17.2, T-SEC-020): se importa la SecurityConfig real, sin desactivar filtros.
// Rutas públicas se prueban como anónimo; las protegidas con @WithMockUser y el rol mínimo de la matriz §11.
// La matriz completa (401/403 por rol) está en security/SeguridadRutasTest.
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired//simula el get
    private MockMvc mockMvc;

    // SecurityConfig la necesita para el filtro JWT y el login. Aquí no se usan tokens: basta un mock
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private UserService userService;//simula el service

    // PRD Seguridad §9.3: POST /api/users ahora delega en AuthService (usuario + credencial + ROLE_USER)
    @MockitoBean
    private AuthService authService;

   
    private final UserResponse andrea = new UserResponse(1L, "andrea", "andrea@pulsepass.com", true);

    // El JSON se escribe a mano (text block) para ver exactamente lo que enviaria un cliente
    private static final String VALID_JSON = """
            {
              "username": "andrea",
              "email": "andrea@pulsepass.com",
              "firstName": "Andrea",
              "lastName": "Gomez",
              "phone": "3001234567",
              "city": "Santa Marta",
              "birthDate": "2000-05-10",
              "password": "UnaFraseSeguraLarga2026!"
            }
            """;

    // Lo que Spring deberia construir a partir de VALID_JSON. Los records comparan por valor,
    // asi que verify(...) puede comprobar que el Service recibio exactamente estos datos
    private final RegisterUserRequest validRequest = new RegisterUserRequest(
            "andrea", "andrea@pulsepass.com", "Andrea", "Gomez",
            "3001234567", "Santa Marta", LocalDate.of(2000, 5, 10), "UnaFraseSeguraLarga2026!");

    @Test
    void register_requestValido_retorna201() throws Exception { 
        when(authService.register(validRequest)).thenReturn(andrea);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@pulsepass.com"));

        verify(authService).register(validRequest);
    }

    @Test
    void register_emailInvalido_retorna400SinLlamarAlService() throws Exception { 
        String json = VALID_JSON.replace("andrea@pulsepass.com", "no-es-un-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(authService, never()).register(any());
    }

    @Test
    void register_usernameDuplicado_retorna409() throws Exception { //Usuario duplicado
        when(authService.register(validRequest))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Username already exists: andrea"))
                .andExpect(jsonPath("$.details").isEmpty());

        verify(authService).register(validRequest);
    }

    @Test
    @WithMockUser(roles = "USER")
    void findByEmail_retorna200() throws Exception { 
        when(userService.findByEmail("andrea@pulsepass.com")).thenReturn(andrea);

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@pulsepass.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@pulsepass.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).findByEmail("andrea@pulsepass.com");
    }

    @Test
    @WithMockUser(roles = "USER")
    void findByUsername_retorna200() throws Exception { // TEST-CTRL-USR-005
        when(userService.findByUsername("andrea")).thenReturn(andrea);

        mockMvc.perform(get("/api/users/by-username").param("username", "andrea"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"));

        verify(userService).findByUsername("andrea");
    }

    // --- Casos extra de validacion de entrada (PRD Controladores 11.4) ---

    @Test
    void register_camposObligatoriosVacios_retorna400ConTodosLosCampos() throws Exception { // QT-CTRL-010
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.username").value("Username is required"))
                .andExpect(jsonPath("$.details.email").value("Email is required"))
                .andExpect(jsonPath("$.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));

        verify(authService, never()).register(any());
    }

    @Test
    void register_jsonMalFormado_retorna400SinLlamarAlService() throws Exception { // PRD 11.4 "JSON mal formado"
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"username\": \"andrea\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(authService, never()).register(any());
    }

    // --- Contraseña en el registro (PRD Seguridad §9.3) ---

    @Test
    void register_respuestaNoExponeLaContrasena() throws Exception { // UC-SEC-01, T-SEC-019
        when(authService.register(validRequest)).thenReturn(andrea);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("UnaFraseSeguraLarga2026!"))));
    }

    @Test
    void register_contrasenaCorta_retorna400SinLlamarAlService() throws Exception { // §9.3 mínimo 12
        String json = VALID_JSON.replace("UnaFraseSeguraLarga2026!", "corta123");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value("Password must have at least 12 characters"));

        verify(authService, never()).register(any());
    }

    @Test
    void register_contrasenaMayorA72Bytes_retorna400SinLlamarAlService() throws Exception { // §9.3 BCrypt
        // 37 "ñ" = 37 caracteres (pasa el mínimo de 12) pero 74 bytes en UTF-8
        String json = VALID_JSON.replace("UnaFraseSeguraLarga2026!", "ñ".repeat(37));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.passwordWithinBcryptLimit").value("Password must not exceed 72 bytes"));

        verify(authService, never()).register(any());
    }
}
