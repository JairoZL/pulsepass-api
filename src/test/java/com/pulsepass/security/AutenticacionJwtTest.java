package com.pulsepass.security;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PRD Seguridad §17.1 (JwtAuthenticationFilterTest) y §17.2 (T-SEC-005..008, 016, 019).
 * Filtro JWT y JwtService REALES (clave de src/test/resources/config/application.yaml).
 * CustomUserDetailsService y los Services son mocks: aquí se prueba el camino HTTP del token,
 * no la base de datos (eso está en integration/RegistroLoginIntegracionTest).
 */
@WebMvcTest
@Import(SecurityConfig.class)
class AutenticacionJwtTest {

    private static final String RUTA_PROTEGIDA = "/api/users/by-username";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JwtProperties jwtProperties;

    @MockitoBean private CustomUserDetailsService userDetailsService;
    @MockitoBean private AuthService authService;
    @MockitoBean private UserService userService;
    @MockitoBean private VenueService venueService;
    @MockitoBean private EventService eventService;
    @MockitoBean private ArtistService artistService;
    @MockitoBean private TicketService ticketService;

    private SecurityPrincipal andrea(boolean activa, String... roles) {
        return new SecurityPrincipal(1L, "andrea", "$2a$10$hashFicticio", activa,
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
    }

    private ResultActions llamarRutaProtegida(String authorization) throws Exception {
        return mockMvc.perform(get(RUTA_PROTEGIDA).param("username", "andrea")
                .header(HttpHeaders.AUTHORIZATION, authorization));
    }

    private void esperar401Uniforme(ResultActions resultado) throws Exception {
        resultado.andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(RestAuthenticationEntryPoint.MESSAGE))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    // ---------- Login (POST /api/auth/login) ----------

    @Test
    @DisplayName("T-SEC-005: login correcto -> 200 con accessToken, tokenType Bearer y expiresIn 900")
    void login_correcto_devuelveToken() throws Exception {
        when(authService.login(new LoginRequest("andrea", "UnaFraseSeguraLarga2026!")))
                .thenReturn(new AuthResponse("jwt-firmado", "Bearer", 900));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        { "identifier": "andrea", "password": "UnaFraseSeguraLarga2026!" }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-firmado"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("T-SEC-006 / T-SEC-019: login fallido -> 401 genérico, sin contraseña en la respuesta")
    void login_fallido_retorna401Generico() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        { "identifier": "andrea", "password": "ContrasenaEquivocada1" }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                .andExpect(content().string(not(containsString("ContrasenaEquivocada1"))));
    }

    @Test
    @DisplayName("Login con body incompleto -> 400 de Bean Validation sin llamar a AuthService")
    void login_bodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.identifier").value("Identifier is required"))
                .andExpect(jsonPath("$.details.password").value("Password is required"));

        verify(authService, never()).login(any());
    }

    // ---------- Bearer válido ----------

    @Test
    @DisplayName("FR-SEC-AUTH-009: Bearer válido -> el filtro carga al usuario por el sub y deja pasar")
    void bearerValido_autentica() throws Exception {
        when(userDetailsService.loadUserById(1L)).thenReturn(andrea(true, "USER"));

        llamarRutaProtegida("Bearer " + jwtService.generateToken(1L))
                .andExpect(status().isOk());

        verify(userDetailsService).loadUserById(1L);
        verify(userService).findByUsername("andrea");
    }

    @Test
    @DisplayName("FR-SEC-AUTH-010: /api/auth/me recibe el principal del token, ya sin el hash")
    void me_conBearer_recibeElPrincipalDelToken() throws Exception {
        when(userDetailsService.loadUserById(1L)).thenReturn(andrea(true, "USER"));
        when(authService.me(any())).thenReturn(new AuthenticatedUserResponse(1L, "andrea", List.of("USER")));

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roles[0]").value("USER"));

        ArgumentCaptor<SecurityPrincipal> captor = ArgumentCaptor.forClass(SecurityPrincipal.class);
        verify(authService).me(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getPassword()).isNull();   // NFR-SEC-001: el filtro borró el hash
    }

    @Test
    @DisplayName("FR-SEC-AUTH-010: /api/auth/me sin token -> 401")
    void me_sinToken_retorna401() throws Exception {
        esperar401Uniforme(mockMvc.perform(get("/api/auth/me")));
        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("§12.1: los roles salen de la base, no del token -> USER no puede crear eventos")
    void rolesDesdeLaBase_userNoPuedeCrearEventos() throws Exception {
        when(userDetailsService.loadUserById(1L)).thenReturn(andrea(true, "USER"));

        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(1L)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(eventService);
    }

    // ---------- Bearer inválido: 401 uniforme y el negocio nunca se ejecuta ----------

    @Test
    @DisplayName("T-SEC-007: token vencido -> 401 con ErrorResponse")
    void tokenVencido_retorna401() throws Exception {
        // Token emitido hace 1 hora con duración de 15 minutos
        JwtService haceUnaHora = new JwtService(jwtProperties,
                Clock.offset(Clock.systemUTC(), Duration.ofHours(-1)));

        esperar401Uniforme(llamarRutaProtegida("Bearer " + haceUnaHora.generateToken(1L)));
        verifyNoInteractions(userDetailsService, userService);
    }

    @Test
    @DisplayName("T-SEC-008: firma alterada -> 401 sin invocar Service")
    void firmaAlterada_retorna401() throws Exception {
        String token = jwtService.generateToken(1L);
        int posicion = token.lastIndexOf('.') + 10;
        char nuevo = token.charAt(posicion) == 'A' ? 'B' : 'A';
        String alterado = token.substring(0, posicion) + nuevo + token.substring(posicion + 1);

        esperar401Uniforme(llamarRutaProtegida("Bearer " + alterado));
        verifyNoInteractions(userDetailsService, userService);
    }

    @Test
    @DisplayName("UC-SEC-09: token de otro issuer -> 401")
    void otroIssuer_retorna401() throws Exception {
        JwtService otroEmisor = new JwtService(
                new JwtProperties("otro-emisor", jwtProperties.secretBase64(), jwtProperties.accessTokenTtl()),
                Clock.systemUTC());

        esperar401Uniforme(llamarRutaProtegida("Bearer " + otroEmisor.generateToken(1L)));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("NFR-SEC-002: texto basura como token -> 401, nunca 500")
    void tokenBasura_retorna401() throws Exception {
        esperar401Uniforme(llamarRutaProtegida("Bearer esto-no-es-un-jwt"));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("'Bearer ' sin token -> 401")
    void bearerVacio_retorna401() throws Exception {
        esperar401Uniforme(llamarRutaProtegida("Bearer "));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("§17.1: dos headers Authorization -> 401 (no se adivina cuál usar)")
    void dobleHeader_retorna401() throws Exception {
        String token = jwtService.generateToken(1L);

        esperar401Uniforme(mockMvc.perform(get(RUTA_PROTEGIDA).param("username", "andrea")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token, "Bearer " + token)));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-007: token válido en la URL (?access_token=) no autentica -> 401")
    void tokenEnQueryString_noAutentica() throws Exception {
        esperar401Uniforme(mockMvc.perform(get(RUTA_PROTEGIDA).param("username", "andrea")
                .param("access_token", jwtService.generateToken(1L))));
        verifyNoInteractions(userDetailsService, userService);
    }

    @Test
    @DisplayName("T-SEC-016 / UC-SEC-10: token válido de un usuario desactivado después -> 401")
    void usuarioDesactivado_retorna401() throws Exception {
        when(userDetailsService.loadUserById(1L)).thenReturn(andrea(false, "USER"));

        esperar401Uniforme(llamarRutaProtegida("Bearer " + jwtService.generateToken(1L)));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Token válido de un usuario que ya no existe -> 401")
    void usuarioInexistente_retorna401() throws Exception {
        when(userDetailsService.loadUserById(1L)).thenThrow(new UsernameNotFoundException("Invalid credentials"));

        esperar401Uniforme(llamarRutaProtegida("Bearer " + jwtService.generateToken(1L)));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("T-SEC-019: la respuesta 401 no devuelve el token recibido")
    void respuesta401_noContieneElToken() throws Exception {
        String token = jwtService.generateToken(1L) + "x";

        llamarRutaProtegida("Bearer " + token)
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString(token))));
    }

    @Test
    @DisplayName("NFR-SEC-006: la respuesta 401 trae X-Request-Id para encontrar el evento en los logs")
    void respuesta401_traeRequestId() throws Exception {
        mockMvc.perform(get(RUTA_PROTEGIDA).param("username", "andrea"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists(RequestIdFilter.HEADER));
    }

    // ---------- Rutas públicas ----------

    @Test
    @DisplayName("§14.3 adv. 5: Bearer inválido en ruta pública -> 401 (nunca se trata como anónimo)")
    void tokenInvalidoEnRutaPublica_retorna401() throws Exception {
        esperar401Uniforme(mockMvc.perform(get("/api/venues/active")
                .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt")));
        verifyNoInteractions(venueService);
    }

    @Test
    @DisplayName("Header de otro esquema (Basic) en ruta pública -> se ignora y pasa como anónimo")
    void esquemaBasicEnRutaPublica_pasaComoAnonimo() throws Exception {
        mockMvc.perform(get("/api/venues/active")
                        .header(HttpHeaders.AUTHORIZATION, "Basic YW5kcmVhOmNsYXZl"))
                .andExpect(status().isOk());
    }
}
