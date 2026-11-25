package com.pulsepass.service;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.InvalidCredentialsException;
import com.pulsepass.model.Role;
import com.pulsepass.model.User;
import com.pulsepass.model.UserCredential;
import com.pulsepass.model.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Seguridad §17.1): AuthService real + dependencias mock.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";
    private static final String HASH = "$2a$10$hashFicticioSoloParaPruebas";

    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserService userService;
    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository userCredentialRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private SecurityPrincipal principal(String... roles) {
        return new SecurityPrincipal(1L, "andrea", HASH, true,
                Arrays.stream(roles).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
    }

    private RegisterUserRequest registro() {
        return new RegisterUserRequest("andrea", "andrea@pulsepass.com", "Andrea", "Gomez",
                "3001234567", "Santa Marta", LocalDate.of(2000, 5, 10), PASSWORD);
    }

    // ---------- Login ----------

    @Test
    @DisplayName("FR-SEC-AUTH-002: credenciales correctas -> token Bearer con expiresIn 900")
    void login_credencialesCorrectas_devuelveToken() {
        SecurityPrincipal andrea = principal("USER");
        Authentication autenticado =
                UsernamePasswordAuthenticationToken.authenticated(andrea, null, andrea.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(autenticado);
        when(jwtService.generateToken(1L)).thenReturn("jwt-firmado");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthResponse respuesta = authService.login(new LoginRequest("andrea", PASSWORD));

        assertThat(respuesta).isEqualTo(new AuthResponse("jwt-firmado", "Bearer", 900));
        // El AuthenticationManager recibió exactamente lo que escribió el usuario
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("andrea");
        assertThat(captor.getValue().getCredentials()).isEqualTo(PASSWORD);
        assertThat(captor.getValue().isAuthenticated()).isFalse();
    }

    @Test
    @DisplayName("FR-SEC-AUTH-003: contraseña incorrecta -> 401 genérico y NO se emite token")
    void login_contrasenaIncorrecta_noEmiteToken() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", "otraContrasena123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    @DisplayName("FR-SEC-AUTH-004: usuario inactivo -> mismo 401 genérico y NO se emite token")
    void login_usuarioInactivo_mismoMensajeGenerico() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales inválidas");
        verify(jwtService, never()).generateToken(anyLong());
    }

    @Test
    @DisplayName("§9.3: contraseña de más de 72 bytes -> rechazo sin llegar a BCrypt")
    void login_contrasenaMayorA72Bytes_noAutentica() {
        String larga = "ñ".repeat(37); // 37 caracteres, 74 bytes en UTF-8

        assertThatThrownBy(() -> authService.login(new LoginRequest("andrea", larga)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(authenticationManager, jwtService);
    }

    // ---------- Registro ----------

    @Test
    @DisplayName("FR-SEC-AUTH-001: registro -> UserService crea el usuario, se guarda el HASH y ROLE_USER")
    void register_valido_guardaHashYRolUser() {
        User andrea = new User("andrea", "andrea@pulsepass.com");
        ReflectionTestUtils.setField(andrea, "id", 1L);
        UserResponse creado = new UserResponse(1L, "andrea", "andrea@pulsepass.com", true);
        when(userService.register(registro())).thenReturn(creado);
        when(userRepository.findById(1L)).thenReturn(Optional.of(andrea));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);

        UserResponse respuesta = authService.register(registro());

        assertThat(respuesta).isEqualTo(creado);

        ArgumentCaptor<UserCredential> credencial = ArgumentCaptor.forClass(UserCredential.class);
        verify(userCredentialRepository).save(credencial.capture());
        assertThat(credencial.getValue().getUser()).isSameAs(andrea);
        assertThat(credencial.getValue().getPasswordHash())
                .isEqualTo(HASH)
                .isNotEqualTo(PASSWORD);           // SEC-003: nunca texto plano

        ArgumentCaptor<UserRole> rol = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository).save(rol.capture());
        assertThat(rol.getValue().getRole()).isEqualTo(Role.USER); // NFR-SEC-009: nunca STAFF/ADMIN
    }

    @Test
    @DisplayName("Registro con username duplicado -> se propaga el 409 y no se guarda credencial ni rol")
    void register_duplicado_noGuardaCredencialNiRol() {
        when(userService.register(registro()))
                .thenThrow(new DuplicateResourceException("Username already exists: andrea"));

        assertThatThrownBy(() -> authService.register(registro()))
                .isInstanceOf(DuplicateResourceException.class);
        verifyNoInteractions(userCredentialRepository, userRoleRepository, passwordEncoder);
    }

    // ---------- /me ----------

    @Test
    @DisplayName("FR-SEC-AUTH-010: /me devuelve id, username y roles sin prefijo, ordenados")
    void me_devuelveDatosMinimos() {
        AuthenticatedUserResponse respuesta = authService.me(principal("USER", "STAFF"));

        assertThat(respuesta.id()).isEqualTo(1L);
        assertThat(respuesta.username()).isEqualTo("andrea");
        assertThat(respuesta.roles()).isEqualTo(List.of("STAFF", "USER"));
    }
}
