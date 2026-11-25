package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

// 403: la identidad es válida pero no tiene permiso (rol insuficiente o recurso ajeno).
public class RestAccessDeniedHandler implements AccessDeniedHandler {

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
        errorWriter.write(response, HttpStatus.FORBIDDEN, MESSAGE);
    }
}
