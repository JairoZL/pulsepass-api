package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// PRD Seguridad §7.1, §12 y §14.4: emite y valida JWT firmados con HS256.
// No consulta la base ni ejecuta negocio: solo firma, claims y fechas.
// No lleva @Service: se registra como @Bean en SecurityConfig (así lo trae cualquier test que importe SecurityConfig).
public class JwtService {

    // HS256 necesita una clave de al menos 256 bits = 32 bytes (§12.2)
    private static final int MIN_KEY_BYTES = 32;
    private static final String EXPECTED_ALGORITHM = "HS256";

    private final SecretKey key;
    private final String issuer;
    private final Duration accessTokenTtl;
    private final Clock clock;
    // El parser es inmutable y seguro entre hilos: se construye una sola vez
    private final JwtParser parser;

    // Clock se recibe por parámetro para que los tests puedan "mover el reloj" (probar expiración)
    public JwtService(JwtProperties properties, Clock clock) {
        if (properties.issuer() == null || properties.issuer().isBlank()) {
            throw new IllegalStateException("Falta pulsepass.security.jwt.issuer");
        }
        Duration ttl = properties.accessTokenTtl();
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalStateException("pulsepass.security.jwt.access-token-ttl debe ser positivo");
        }
        this.key = buildKey(properties.secretBase64());
        this.issuer = properties.issuer();
        this.accessTokenTtl = ttl;
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)                                  // verifica la firma ANTES de leer claims
                .requireIssuer(issuer)                            // iss distinto -> rechazo (SEC-005)
                .clock(() -> Date.from(clock.instant()))          // exp se compara contra este reloj
                .build();
    }

    // La aplicación NO arranca si la clave falta, no es Base64 o es corta (§12.2)
    private static SecretKey buildKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("Falta la clave JWT: define la variable PULSEPASS_JWT_SECRET_BASE64");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secretBase64.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException("PULSEPASS_JWT_SECRET_BASE64 no es Base64 válido");
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException(
                    "La clave JWT debe tener al menos " + MIN_KEY_BYTES + " bytes; tiene " + bytes.length);
        }
        // Se fija HmacSHA256 a propósito. Keys.hmacShaKeyFor (el del §14.4) elige el algoritmo
        // según el largo de la clave: con 64 bytes daría HmacSHA512. Así siempre es HS256 (§12.2)
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // FR-SEC-AUTH-005: sub, iss, iat, exp y jti. Sin roles ni datos personales en el payload (§6, §12.1)
    public String generateToken(Long userId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())                       // id persistente, no el correo
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenTtl)))
                .id(UUID.randomUUID().toString())                 // jti: identificador único del token
                .signWith(key, Jwts.SIG.HS256)                    // algoritmo explícito
                .compact();
    }

    // Para AuthResponse.expiresIn (900 con PT15M)
    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    // FR-SEC-AUTH-006: devuelve el id del usuario o lanza excepción. Nunca devuelve null.
    // Excepciones posibles (todas las atrapa el filtro y responde 401):
    //   ExpiredJwtException, SignatureException, MalformedJwtException, UnsupportedJwtException,
    //   IncorrectClaimException (iss) -> todas heredan de JwtException
    //   IllegalArgumentException      -> token vacío
    public Long validateAndGetUserId(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token vacío");
        }
        // parseSignedClaims exige firma: un token sin firma (alg=none) se rechaza aquí
        Jws<Claims> jws = parser.parseSignedClaims(token);

        // Algoritmo esperado: solo HS256 (§12.2)
        if (!EXPECTED_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
            throw new UnsupportedJwtException("Algoritmo no permitido");
        }

        Claims claims = jws.getPayload();
        // JJWT solo valida exp si viene. El PRD exige que exp, iat y jti existan (§12.1, §14.4)
        if (claims.getExpiration() == null) {
            throw new MalformedJwtException("Falta exp");
        }
        if (claims.getIssuedAt() == null || claims.getIssuedAt().toInstant().isAfter(clock.instant())) {
            throw new MalformedJwtException("iat ausente o en el futuro");
        }
        if (claims.getId() == null || claims.getId().isBlank()) {
            throw new MalformedJwtException("Falta jti");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("Falta sub");
        }
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new MalformedJwtException("sub no es un id válido", ex);
        }
    }
}
