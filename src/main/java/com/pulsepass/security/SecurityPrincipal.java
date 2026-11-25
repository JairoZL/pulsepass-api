package com.pulsepass.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// PRD Seguridad §6 "Principal": la identidad ya comprobada de quien hace la solicitud.
// Spring Security trabaja con la interfaz UserDetails; esta clase la implementa y además
// guarda el id persistente del usuario, que es lo que se compara al verificar propiedad
// (SEC-006: nunca el email ni el username que manda el cliente).
public class SecurityPrincipal implements UserDetails, CredentialsContainer {

    private final Long userId;
    private final String username;
    // No es final: eraseCredentials() lo borra después de autenticar
    private String passwordHash;
    private final boolean enabled;
    private final List<GrantedAuthority> authorities;

    public SecurityPrincipal(Long userId,
                             String username,
                             String passwordHash,
                             boolean enabled,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        // Copia inmutable: nadie puede agregarse un rol modificando la lista
        this.authorities = List.copyOf(authorities);
    }

    public Long getUserId() {
        return userId;
    }

    // Authorities con prefijo: ROLE_USER, ROLE_STAFF, ROLE_ADMIN. hasRole("ADMIN") busca ROLE_ADMIN
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // DaoAuthenticationProvider compara este hash con la contraseña del login usando BCrypt
    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // users.active = false -> la cuenta queda deshabilitada (FR-SEC-AUTH-004, SEC-011)
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // El dominio no modela expiración ni bloqueo de cuentas: siempre true
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Spring Security llama a este método al terminar el login: el hash no queda
    // guardado en el objeto autenticado (NFR-SEC-001)
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    // toString propio, sin el hash, por si el principal llega a un log
    @Override
    public String toString() {
        return "SecurityPrincipal[userId=" + userId + ", username=" + username
                + ", enabled=" + enabled + ", authorities=" + authorities + "]";
    }
}
