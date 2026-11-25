package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

// 403: la identidad es válida pero no tiene permiso (rol insuficiente o recurso ajeno).
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(RestAccessDeniedHandler.class);

    // Texto del ejemplo de 403 del PRD Seguridad §13
    static final String MESSAGE = "No tiene permisos para realizar esta operación";

    private final SecurityErrorWriter errorWriter;

    public RestAccessDeniedHandler(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        // NFR-SEC-006: se registra la denegación (quién y qué ruta). El request ID lo agrega el patrón de log
        log.warn("Acceso denegado (403): {} {} userId={}", request.getMethod(), request.getRequestURI(), currentUserId());
        errorWriter.write(response, HttpStatus.FORBIDDEN, MESSAGE);
    }

    private Object currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SecurityPrincipal principal) {
            return principal.getUserId();
        }
        return "desconocido";
    }
}
