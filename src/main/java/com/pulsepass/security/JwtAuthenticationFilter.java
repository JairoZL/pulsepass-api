package com.pulsepass.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

// PRD Seguridad §7.1 y §14.3: lee el header Authorization, valida el JWT y deja la identidad
// en el SecurityContext. No crea cuentas ni guarda tokens.
// OncePerRequestFilter garantiza que corre una sola vez por request.
// No es un @Component a propósito: si fuera un bean, Spring Boot además lo registraría como
// filtro del servidor y correría fuera de la cadena de seguridad. Lo crea SecurityConfig.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   CustomUserDetailsService userDetailsService,
                                   AuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // FR-SEC-AUTH-007: el token SOLO se lee del header. Nunca de ?token= en la URL
        List<String> headers = Collections.list(request.getHeaders(HttpHeaders.AUTHORIZATION));

        // 1. Sin header: sigue como anónimo y SecurityConfig decide si la ruta es pública (§14.3 adv. 5)
        if (headers.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Si ya hay una identidad en el contexto, no se reemplaza (§17.1)
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated() && !(existing instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractBearerToken(headers);
            // 3. Header de otro tipo (ej. "Basic ..."): no es nuestro, sigue como anónimo
            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 4. Firma, issuer y fechas. Si algo falla, lanza excepción (no devuelve null)
            Long userId = jwtService.validateAndGetUserId(token);

            // 5. Roles y estado ACTUALES desde la base, no desde el token (FR-SEC-AUTH-009, §12.1)
            SecurityPrincipal principal = userDetailsService.loadUserById(userId);

            // 6. Usuario desactivado después de recibir el token: pierde acceso ya (SEC-011, UC-SEC-10)
            if (!principal.isEnabled()) {
                throw new DisabledException("Usuario inactivo");
            }

            // El hash no hace falta durante el request: se borra del principal (NFR-SEC-001)
            principal.eraseCredentials();

            // 7. Identidad autenticada: credentials = null porque no hay contraseña en este flujo
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

        } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
            // 8. Bearer inválido: NUNCA se deja pasar como anónimo (§14.3 adv. 5), se responde 401
            //    aunque la ruta sea pública. Al log va el tipo de error, nunca el token (§12.2, NFR-SEC-006)
            SecurityContextHolder.clearContext();
            log.warn("JWT rechazado en {} {}: {}", request.getMethod(), request.getRequestURI(),
                    ex.getClass().getSimpleName());
            authenticationEntryPoint.commence(request, response,
                    new BadCredentialsException("Token inválido", ex));
            return;
        }

        filterChain.doFilter(request, response);
    }

    // Devuelve el token, null si el header no es Bearer, o lanza excepción si es sospechoso
    private String extractBearerToken(List<String> headers) {
        // Dos headers Authorization en el mismo request: no se adivina cuál usar (§17.1)
        if (headers.size() > 1) {
            throw new BadCredentialsException("Más de un header Authorization");
        }
        String header = headers.get(0);
        // El esquema "Bearer" no distingue mayúsculas (RFC 6750)
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("Bearer sin token");
        }
        return token;
    }
}
