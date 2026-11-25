package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.service.UserService;
import org.junit.jupiter.api.Test;
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
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // Usuaria de referencia: Andrea, adulta y activa (PRD Controladores 23.4)
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
              "birthDate": "2000-05-10"
            }
            """;

    // Lo que Spring deberia construir a partir de VALID_JSON. Los records comparan por valor,
    // asi que verify(...) puede comprobar que el Service recibio exactamente estos datos
    private final RegisterUserRequest validRequest = new RegisterUserRequest(
            "andrea", "andrea@pulsepass.com", "Andrea", "Gomez",
            "3001234567", "Santa Marta", LocalDate.of(2000, 5, 10));

    @Test
    void register_requestValido_retorna201() throws Exception { // TEST-CTRL-USR-001 / AC-CTRL-007
        when(userService.register(validRequest)).thenReturn(andrea);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@pulsepass.com"));

        verify(userService).register(validRequest);
    }

    @Test
    void register_emailInvalido_retorna400SinLlamarAlService() throws Exception { // TEST-CTRL-USR-002 / QT-CTRL-007, QT-CTRL-010
        String json = VALID_JSON.replace("andrea@pulsepass.com", "no-es-un-email");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(userService, never()).register(any());
    }

    @Test
    void register_usernameDuplicado_retorna409() throws Exception { // TEST-CTRL-USR-003 / AC-CTRL-008
        when(userService.register(validRequest))
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

        verify(userService).register(validRequest);
    }

    @Test
    void findByEmail_retorna200() throws Exception { // TEST-CTRL-USR-004
        when(userService.findByEmail("andrea@pulsepass.com")).thenReturn(andrea);

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@pulsepass.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("andrea@pulsepass.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).findByEmail("andrea@pulsepass.com");
    }

    @Test
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
                .andExpect(jsonPath("$.details.birthDate").value("Birth date is required"));

        verify(userService, never()).register(any());
    }

    @Test
    void register_jsonMalFormado_retorna400SinLlamarAlService() throws Exception { // PRD 11.4 "JSON mal formado"
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"username\": \"andrea\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Malformed request body"));

        verify(userService, never()).register(any());
    }
}
