package com.pulsepass.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

// 401: la solicitud no trae una identidad válida (sin token, token vencido o alterado).
// Spring Security llama a commence() cuando un anónimo intenta entrar a una ruta protegida.
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    // Mensaje fijo: no se usa authException.getMessage() para no explicar por qué falló (SEC-009)
    static final String MESSAGE = "Se requiere autenticación";

    private final SecurityErrorWriter errorWriter;

    public RestAuthenticationEntryPoint(SecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        errorWriter.write(response, HttpStatus.UNAUTHORIZED, MESSAGE);
    }
}
