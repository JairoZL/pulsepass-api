package com.pulsepass.security;

import com.pulsepass.model.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Unit test puro (PRD Seguridad §17.1): AuthorizationService real + repositorios mock.
// El "usuario autenticado" se simula poniendo un SecurityPrincipal en el SecurityContext,
// igual que lo hace JwtAuthenticationFilter.
@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final Long ANDREA_ID = 1L;
    private static final Long CARLOS_ID = 2L;

    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;

    @InjectMocks
    private AuthorizationService authz;

    // Cada prueba deja el contexto limpio: SecurityContextHolder es estático (por hilo)
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long userId, String username) {
        SecurityPrincipal principal = new SecurityPrincipal(userId, username, null, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private User usuario(Long id, String username) {
        User user = new User(username, username + "@pulsepass.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    // ---------- isSelfEmail ----------

    @Test
    @DisplayName("SEC-006: su propio correo -> true (se compara por id, no por el texto)")
    void isSelfEmail_propio_true() {
        autenticarComo(ANDREA_ID, "andrea");
        when(userRepository.findByEmailIgnoreCase("ANDREA@pulsepass.com"))
                .thenReturn(Optional.of(usuario(ANDREA_ID, "andrea")));

        assertThat(authz.isSelfEmail("ANDREA@pulsepass.com")).isTrue();
    }

    @Test
    @DisplayName("T-SEC-010: correo de otro usuario -> false")
    void isSelfEmail_ajeno_false() {
        autenticarComo(ANDREA_ID, "andrea");
        when(userRepository.findByEmailIgnoreCase("carlos@pulsepass.com"))
                .thenReturn(Optional.of(usuario(CARLOS_ID, "carlos")));

        assertThat(authz.isSelfEmail("carlos@pulsepass.com")).isFalse();
    }

    @Test
    @DisplayName("Correo inexistente -> false (403 igual que uno ajeno)")
    void isSelfEmail_inexistente_false() {
        autenticarComo(ANDREA_ID, "andrea");
        when(userRepository.findByEmailIgnoreCase("nadie@pulsepass.com")).thenReturn(Optional.empty());

        assertThat(authz.isSelfEmail("nadie@pulsepass.com")).isFalse();
    }

    @Test
    @DisplayName("Sin autenticación -> false y ni siquiera consulta la base")
    void isSelfEmail_sinAutenticacion_false() {
        assertThat(authz.isSelfEmail("andrea@pulsepass.com")).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Anónimo (AnonymousAuthenticationToken) -> false")
    void isSelfEmail_anonimo_false() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "clave", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(authz.isSelfEmail("andrea@pulsepass.com")).isFalse();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("Correo null -> false")
    void isSelfEmail_null_false() {
        autenticarComo(ANDREA_ID, "andrea");

        assertThat(authz.isSelfEmail(null)).isFalse();
    }

    // ---------- isSelfUsername ----------

    @Test
    @DisplayName("FR-SEC-USR-03: su propio username -> true; el de otro -> false")
    void isSelfUsername_propioYAjeno() {
        autenticarComo(ANDREA_ID, "andrea");
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(usuario(ANDREA_ID, "andrea")));
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(usuario(CARLOS_ID, "carlos")));

        assertThat(authz.isSelfUsername("andrea")).isTrue();
        assertThat(authz.isSelfUsername("carlos")).isFalse();
    }

    // ---------- isTicketOwner ----------

    @Test
    @DisplayName("§11.1: el dueño del ticket se consulta en la base con el id del token")
    void isTicketOwner_consultaPorIdDelToken() {
        autenticarComo(ANDREA_ID, "andrea");
        when(ticketRepository.existsByTicketCodeAndUserId("TKT-ANDREA", ANDREA_ID)).thenReturn(true);
        when(ticketRepository.existsByTicketCodeAndUserId("TKT-CARLOS", ANDREA_ID)).thenReturn(false);

        assertThat(authz.isTicketOwner("TKT-ANDREA")).isTrue();
        assertThat(authz.isTicketOwner("TKT-CARLOS")).isFalse();
    }

    @Test
    @DisplayName("Sin autenticación -> false sin consultar tickets")
    void isTicketOwner_sinAutenticacion_false() {
        assertThat(authz.isTicketOwner("TKT-ANDREA")).isFalse();
        verifyNoInteractions(ticketRepository);
    }
}
