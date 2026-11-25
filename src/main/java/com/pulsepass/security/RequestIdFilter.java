package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

// PRD Seguridad NFR-SEC-006: cada request lleva un identificador (request ID) que aparece en
// TODAS las líneas de log de ese request (login, token rechazado, 401, 403, 500) y en el header
// X-Request-Id de la respuesta. Si un usuario reporta un error, con ese id se encuentran sus logs.
//
// MDC (Mapped Diagnostic Context) es un mapa por hilo que el logger agrega a cada línea:
// el patrón de log de application.yaml imprime %X{requestId}.
//
// @Order(HIGHEST_PRECEDENCE): corre ANTES que la cadena de Spring Security (orden -100),
// así los logs del filtro JWT y de los handlers 401/403 ya tienen el id.
// A diferencia de JwtAuthenticationFilter, aquí SÍ conviene que sea un filtro del servidor (@Component).
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    // Solo letras, números y guiones, máximo 64. Un id enviado por el cliente que no cumpla
    // se descarta: evita que alguien meta saltos de línea para falsificar líneas de log
    private static final Pattern VALID_ID = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Si el cliente (o un proxy) ya trae un id válido, se reutiliza para seguir la petición de punta a punta
        String clientId = request.getHeader(HEADER);
        String requestId = (clientId != null && VALID_ID.matcher(clientId).matches())
                ? clientId
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // El hilo vuelve al pool del servidor: si no se limpia, el siguiente request heredaría este id
            MDC.remove(MDC_KEY);
        }
    }
}
