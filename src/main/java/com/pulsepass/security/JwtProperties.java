package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

// PRD Seguridad §14.2 y NFR-SEC-008: emisor, clave y duración salen de la configuración,
// nunca del código (SEC-004). Spring llena este record con las propiedades
// pulsepass.security.jwt.* de application.yaml:
//   issuer           -> pulsepass.security.jwt.issuer
//   secretBase64     -> pulsepass.security.jwt.secret-base64
//   accessTokenTtl   -> pulsepass.security.jwt.access-token-ttl (formato ISO-8601, ej. PT15M = 15 minutos)
// La validación (clave presente y de 32+ bytes) la hace JwtService al arrancar.
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(
        String issuer,
        String secretBase64,
        Duration accessTokenTtl
) {
    // toString propio: el record por defecto imprimiría la clave (NFR-SEC-001)
    @Override
    public String toString() {
        return "JwtProperties[issuer=" + issuer + ", secretBase64=****, accessTokenTtl=" + accessTokenTtl + "]";
    }
}
