package com.pulsepass.security;

import com.pulsepass.model.User;
import com.pulsepass.model.UserCredential;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// PRD Seguridad §7.1: carga identidad, hash, roles y estado del usuario.
// No valida reglas comerciales (eso sigue en los Services).
// Se usa en dos momentos:
//   - login: loadUserByUsername(identificador), lo llama DaoAuthenticationProvider (FR-SEC-AUTH-002)
//   - cada request con JWT: loadUserById(sub), lo llamará el filtro (FR-SEC-AUTH-009, etapa 4)
// En ambos casos los roles y el estado se leen de la base, no del token (§12.1).
@Service
public class CustomUserDetailsService implements UserDetailsService {

    // Mismo mensaje para "no existe" y "sin credencial": no revela cuál de los dos pasó (FR-SEC-AUTH-003)
    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    UserCredentialRepository userCredentialRepository,
                                    UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
    }

    // El login acepta username o correo (FR-SEC-AUTH-002): primero busca por username
    // y, si no existe, por email sin distinguir mayúsculas
    @Override
    @Transactional(readOnly = true)
    public SecurityPrincipal loadUserByUsername(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new UsernameNotFoundException(INVALID_CREDENTIALS);
        }
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmailIgnoreCase(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));
        return toPrincipal(user);
    }

    // El sub del JWT es el id persistente, no el correo (§9.1, §12.1)
    @Transactional(readOnly = true)
    public SecurityPrincipal loadUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));
        return toPrincipal(user);
    }

    // Tres consultas fijas por usuario (user, credencial, roles): sin N+1 (NFR-SEC-003)
    private SecurityPrincipal toPrincipal(User user) {
        // Usuario legado sin credencial: no puede autenticarse (§9.2)
        UserCredential credential = userCredentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(INVALID_CREDENTIALS));

        // Role.ADMIN -> "ROLE_ADMIN": el prefijo que espera hasRole("ADMIN")
        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().name()))
                .toList();

        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                credential.getPasswordHash(),
                Boolean.TRUE.equals(user.getActive()),
                authorities);
    }
}
