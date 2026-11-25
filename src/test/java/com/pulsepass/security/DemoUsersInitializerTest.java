package com.pulsepass.security;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.model.Role;
import com.pulsepass.model.User;
import com.pulsepass.model.UserRole;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Unit test (PRD Seguridad §9.2, NFR-SEC-009): cuentas demo STAFF/ADMIN del perfil dev.
@ExtendWith(MockitoExtension.class)
class DemoUsersInitializerTest {

    private static final String PASSWORD = "UnaFraseSeguraLarga2026!";

    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;

    private DemoUsersInitializer initializer(String staffPassword, String adminPassword) {
        return new DemoUsersInitializer(authService, userRepository, userRoleRepository, staffPassword, adminPassword);
    }

    private User usuario(Long id, String username) {
        User user = new User(username, username + "@pulsepass.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    @DisplayName("Crea laura (USER + STAFF) y admin (USER + ADMIN) pasando por AuthService.register")
    void cuentasNuevas_seCreanConSuRolExtra() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(authService.register(any())).thenAnswer(inv -> {
            RegisterUserRequest r = inv.getArgument(0);
            long id = r.username().equals("laura") ? 10L : 20L;
            return new UserResponse(id, r.username(), r.email(), true);
        });
        when(userRepository.findById(10L)).thenReturn(Optional.of(usuario(10L, "laura")));
        when(userRepository.findById(20L)).thenReturn(Optional.of(usuario(20L, "admin")));

        initializer(PASSWORD, PASSWORD).run(null);

        // AuthService.register guarda el hash y ROLE_USER; aquí solo se agrega el rol extra
        ArgumentCaptor<UserRole> roles = ArgumentCaptor.forClass(UserRole.class);
        verify(userRoleRepository, times(2)).save(roles.capture());
        assertThat(roles.getAllValues()).extracting(UserRole::getRole)
                .containsExactly(Role.STAFF, Role.ADMIN);
    }

    @Test
    @DisplayName("Si la cuenta ya existe no se toca (se puede arrancar varias veces)")
    void cuentasExistentes_noSeModifican() {
        when(userRepository.findByUsername("laura")).thenReturn(Optional.of(usuario(10L, "laura")));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(usuario(20L, "admin")));

        initializer(PASSWORD, PASSWORD).run(null);

        verify(authService, never()).register(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("§12.2: sin contraseña en .env la cuenta no se crea (no hay contraseña por defecto)")
    void sinContrasena_noSeCrea() {
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        initializer("", "  ").run(null);

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("§9.3: contraseña demo de menos de 12 caracteres -> la app no arranca")
    void contrasenaCorta_falla() {
        when(userRepository.findByUsername("laura")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> initializer("corta123", PASSWORD).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("12");
        verify(authService, never()).register(any());
    }
}
