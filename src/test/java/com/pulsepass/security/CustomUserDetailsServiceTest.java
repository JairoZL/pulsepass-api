package com.pulsepass.security;

import com.pulsepass.model.Role;
import com.pulsepass.model.User;
import com.pulsepass.model.UserCredential;
import com.pulsepass.model.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Seguridad §17.1): CustomUserDetailsService real + repositorios mock.
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    private static final Long USER_ID = 1L;
    private static final String HASH = "$2a$10$hashFicticioSoloParaPruebas";

    @Mock private UserRepository userRepository;
    @Mock private UserCredentialRepository userCredentialRepository;
    @Mock private UserRoleRepository userRoleRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User andrea;

    @BeforeEach
    void setUp() {
        andrea = new User("andrea", "andrea@pulsepass.com");
        // User no tiene setId: el id lo pone la base. En un unit test se asigna por reflexión
        ReflectionTestUtils.setField(andrea, "id", USER_ID);
    }

    private void conCredencialYRoles(Role... roles) {
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(andrea, HASH)));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(Arrays.stream(roles).map(r -> new UserRole(andrea, r)).toList());
    }

    private List<String> nombres(SecurityPrincipal principal) {
        return principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    @DisplayName("FR-SEC-AUTH-002: login por username -> principal con id, hash y ROLE_USER")
    void loadUserByUsername_porUsername_construyeElPrincipal() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        conCredencialYRoles(Role.USER);

        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        assertThat(principal.getUserId()).isEqualTo(USER_ID);
        assertThat(principal.getUsername()).isEqualTo("andrea");
        assertThat(principal.getPassword()).isEqualTo(HASH);
        assertThat(principal.isEnabled()).isTrue();
        assertThat(nombres(principal)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("FR-SEC-AUTH-002: login por correo (sin distinguir mayúsculas) también funciona")
    void loadUserByUsername_porEmail_buscaPorCorreo() {
        when(userRepository.findByUsername("ANDREA@pulsepass.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ANDREA@pulsepass.com")).thenReturn(Optional.of(andrea));
        conCredencialYRoles(Role.USER);

        SecurityPrincipal principal = service.loadUserByUsername("ANDREA@pulsepass.com");

        assertThat(principal.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-003: usuario inexistente -> UsernameNotFoundException con mensaje genérico")
    void loadUserByUsername_inexistente_lanzaExcepcionGenerica() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @DisplayName("§9.2: usuario legado sin credencial -> no puede autenticarse (mismo mensaje genérico)")
    void loadUserByUsername_sinCredencial_lanzaExcepcionGenerica() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        when(userCredentialRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("andrea"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Invalid credentials");
        verify(userRoleRepository, never()).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("Identificador vacío -> excepción sin consultar la base")
    void loadUserByUsername_vacio_noConsultaLaBase() {
        assertThatThrownBy(() -> service.loadUserByUsername("  "))
                .isInstanceOf(UsernameNotFoundException.class);
        verify(userRepository, never()).findByUsername("  ");
    }

    @Test
    @DisplayName("FR-SEC-AUTH-004 / SEC-011: usuario inactivo -> principal deshabilitado")
    void loadUserByUsername_usuarioInactivo_principalDeshabilitado() {
        ReflectionTestUtils.setField(andrea, "active", false);
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        conCredencialYRoles(Role.USER);

        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        assertThat(principal.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("§12.1: varios roles -> se convierten en authorities con prefijo ROLE_")
    void loadUserById_variosRoles_authoritiesConPrefijo() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(andrea));
        conCredencialYRoles(Role.USER, Role.STAFF);

        SecurityPrincipal principal = service.loadUserById(USER_ID);

        assertThat(nombres(principal)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_STAFF");
    }

    @Test
    @DisplayName("§12.1: roles actualizados en la base -> el siguiente load ya no trae el rol retirado")
    void loadUserById_rolRetirado_noApareceEnLaSiguienteCarga() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(andrea));
        when(userCredentialRepository.findById(USER_ID))
                .thenReturn(Optional.of(new UserCredential(andrea, HASH)));
        // Primera carga: USER + ADMIN. Segunda carga: le quitaron ADMIN
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(new UserRole(andrea, Role.USER), new UserRole(andrea, Role.ADMIN)))
                .thenReturn(List.of(new UserRole(andrea, Role.USER)));

        SecurityPrincipal antes = service.loadUserById(USER_ID);
        SecurityPrincipal despues = service.loadUserById(USER_ID);

        assertThat(nombres(antes)).contains("ROLE_ADMIN");
        assertThat(nombres(despues)).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("FR-SEC-AUTH-009: id inexistente (usuario borrado) -> excepción")
    void loadUserById_inexistente_lanzaExcepcion() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserById(99L))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("NFR-SEC-001: eraseCredentials borra el hash y toString no lo muestra")
    void principal_noExponeElHash() {
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(andrea));
        conCredencialYRoles(Role.USER);
        SecurityPrincipal principal = service.loadUserByUsername("andrea");

        assertThat(principal.toString()).doesNotContain(HASH);
        principal.eraseCredentials();
        assertThat(principal.getPassword()).isNull();
    }
}
