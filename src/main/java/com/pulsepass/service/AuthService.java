package com.pulsepass.service;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

// PRD Seguridad §7.1: autentica, registra credenciales y emite el token.
// NO reemplaza a UserService: las reglas del usuario (unicidad, fecha de nacimiento) siguen allá.
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    // BCrypt solo usa los primeros 72 bytes de la contraseña (§9.3)
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String ROLE_PREFIX = "ROLE_";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserService userService,
                       UserRepository userRepository,
                       UserCredentialRepository userCredentialRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.userCredentialRepository = userCredentialRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // FR-SEC-AUTH-001 y NFR-SEC-004: User + UserProfile + credencial + ROLE_USER en UNA transacción.
    // userService.register también es @Transactional, pero se UNE a esta transacción (propagación
    // REQUIRED por defecto). Si cualquier paso falla, se revierte todo: no quedan cuentas sin contraseña.
    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        // 1. Reglas de negocio del usuario (unicidad, fecha de nacimiento): las aplica UserService
        UserResponse created = userService.register(request);

        // 2. La entidad ya está en el contexto de persistencia de esta transacción: no hace otra consulta
        User user = userRepository.findById(created.id())
                .orElseThrow(() -> new IllegalStateException("Usuario recién creado no encontrado"));

        // 3. Solo el hash BCrypt se guarda (SEC-003)
        userCredentialRepository.save(new UserCredential(user, passwordEncoder.encode(request.password())));

        // 4. El registro público siempre da USER; STAFF y ADMIN nunca salen de aquí (NFR-SEC-009)
        userRoleRepository.save(new UserRole(user, Role.USER));

        log.info("Registro exitoso: userId={}", user.getId());
        return created;
    }

    // FR-SEC-AUTH-002/003/004: el token se emite SOLO después de autenticar
    public AuthResponse login(LoginRequest request) {
        // Más de 72 bytes nunca puede ser una contraseña válida (el registro no lo permite)
        if (request.password().getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            log.info("Login fallido: contraseña excede 72 bytes");
            throw new InvalidCredentialsException();
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.identifier(), request.password()));
        } catch (AuthenticationException ex) {
            // BadCredentialsException (no existe / contraseña mala) o DisabledException (inactivo):
            // al cliente le llega lo mismo. Al log va el tipo, nunca la contraseña (NFR-SEC-006)
            log.info("Login fallido: {}", ex.getClass().getSimpleName());
            throw new InvalidCredentialsException();
        }

        SecurityPrincipal principal = (SecurityPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getUserId());
        log.info("Login exitoso: userId={}", principal.getUserId());
        return new AuthResponse(token, "Bearer", jwtService.getAccessTokenTtlSeconds());
    }

    // FR-SEC-AUTH-010: datos mínimos del usuario autenticado
    public AuthenticatedUserResponse me(SecurityPrincipal principal) {
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.startsWith(ROLE_PREFIX)
                        ? authority.substring(ROLE_PREFIX.length())
                        : authority)
                .sorted()
                .toList();
        return new AuthenticatedUserResponse(principal.getUserId(), principal.getUsername(), roles);
    }
}
