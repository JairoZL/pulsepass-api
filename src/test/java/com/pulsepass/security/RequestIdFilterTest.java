package com.pulsepass.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unit test (PRD Seguridad NFR-SEC-006): request ID en el MDC durante el request y en el header de respuesta.
class RequestIdFilterTest {

    private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final RequestIdFilter filter = new RequestIdFilter();

    // Ejecuta el filtro y devuelve el request ID que vio el resto de la cadena (controladores, logs)
    private String ejecutar(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> visto = new AtomicReference<>();
        FilterChain cadena = (req, res) -> visto.set(MDC.get(RequestIdFilter.MDC_KEY));
        filter.doFilter(request, response, cadena);
        return visto.get();
    }

    @Test
    @DisplayName("Sin header: genera un UUID, lo pone en el MDC y en X-Request-Id, y luego limpia el MDC")
    void sinHeader_generaUuid() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String id = ejecutar(new MockHttpServletRequest("GET", "/api/venues/active"), response);

        assertThat(id).matches(UUID_REGEX);
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo(id);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("Header válido del cliente: se reutiliza para seguir la petición de punta a punta")
    void headerValido_seReutiliza() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/venues/active");
        request.addHeader(RequestIdFilter.HEADER, "demo-sustentacion-01");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(ejecutar(request, response)).isEqualTo("demo-sustentacion-01");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("demo-sustentacion-01");
    }

    @Test
    @DisplayName("Header con salto de línea (intento de falsificar logs): se descarta y se genera uno nuevo")
    void headerInvalido_seDescarta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/venues/active");
        request.addHeader(RequestIdFilter.HEADER, "abc\nINFO Login exitoso: userId=1");

        String id = ejecutar(request, new MockHttpServletResponse());

        assertThat(id).matches(UUID_REGEX);
    }

    @Test
    @DisplayName("Si el request falla, el MDC igual queda limpio (no contamina el siguiente request)")
    void falla_igualLimpiaElMdc() {
        FilterChain cadenaQueFalla = (req, res) -> {
            throw new ServletException("fallo simulado");
        };

        assertThatThrownBy(() -> filter.doFilter(
                new MockHttpServletRequest("GET", "/api/venues/active"), new MockHttpServletResponse(), cadenaQueFalla))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
    }
}
