package com.pulsepass.security;

import com.pulsepass.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

// PRD Seguridad §13: los errores del filtro de seguridad ocurren ANTES del DispatcherServlet,
// así que @RestControllerAdvice (GlobalExceptionHandler) no los ve. Esta clase escribe a mano
// el mismo ErrorResponse, para que 401 y 403 tengan la misma forma que 400/404/409/500.
public class SecurityErrorWriter {

    // Spring Boot 4 usa Jackson 3 (paquete tools.jackson). Es el mismo JsonMapper que usan
    // los controladores, así las fechas salen con el mismo formato
    private final JsonMapper jsonMapper;

    public SecurityErrorWriter(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        // Si otra parte ya empezó a enviar la respuesta, no se escribe dos veces (§13)
        if (response.isCommitted()) {
            return;
        }
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                Map.of());
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), body);
    }
}
