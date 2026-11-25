package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Unit test puro (PRD Seguridad §17.1): JwtService real, sin Spring.
// El reloj es fijo para que las fechas sean exactas y la expiración se pueda probar sin esperar.
class JwtServiceTest {

    // Claves ficticias, solo para pruebas (§17.3)
    private static final String SECRET_32 = "DXU+b+AJykyWtBceCefd4Zc3ijkT9BK5mPxpUqSulQ8=";
    private static final String OTRO_SECRET_32 = "q6Jm0mJ0m3l8b2kzZ0Z1a3N4c2F5b2xpbmFyZXNlYW4=";
    private static final String SECRET_64 =
            "d2UGpHqsARo70rTzyi1W7FOSBj3mY7QPer1/Dgt7BFrCz//hLK4vRgvcpujfglC8DjR1m+Qt72P/5mU4oqFQSQ==";
    private static final String ISSUER = "pulsepass-api";
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final Instant AHORA = Instant.parse("2026-11-25T12:00:00Z");

    private static Clock relojEn(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private static JwtService servicio(String issuer, String secret, Instant ahora) {
        return new JwtService(new JwtProperties(issuer, secret, TTL), relojEn(ahora));
    }

    private static SecretKey clave(String secretBase64, String algoritmoJca) {
        return new SecretKeySpec(Base64.getDecoder().decode(secretBase64), algoritmoJca);
    }

    private final JwtService jwtService = servicio(ISSUER, SECRET_32, AHORA);

    // ---------- Emisión ----------

    @Test
    @DisplayName("FR-SEC-AUTH-005: el token generado se valida y devuelve el id del usuario")
    void generarYValidar_devuelveElIdDelUsuario() {
        String token = jwtService.generateToken(1L);

        assertThat(jwtService.validateAndGetUserId(token)).isEqualTo(1L);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-005: contiene sub, iss, iat, exp (15 min) y jti, firmado con HS256")
    void generarToken_contieneLosClaimsMinimos() {
        String token = jwtService.generateToken(1L);

        // Se lee con JJWT usando la misma clave y el mismo reloj fijo del servicio
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(clave(SECRET_32, "HmacSHA256"))
                .clock(() -> Date.from(AHORA))
                .build()
                .parseSignedClaims(token);
        Claims claims = jws.getPayload();

        assertThat(jws.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(AHORA);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(AHORA.plus(TTL));
        assertThat(claims.getId()).isNotBlank();
        // §6 y §12.1: nada sensible en el payload (se puede leer sin la clave)
        assertThat(claims).doesNotContainKeys("password", "email", "roles");
    }

    @Test
    @DisplayName("§10.2: expiresIn = 900 segundos con PT15M")
    void ttlEnSegundos_es900() {
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(900);
    }

    @Test
    @DisplayName("jti aleatorio: dos tokens del mismo usuario no son iguales")
    void dosTokens_tienenJtiDistinto() {
        assertThat(jwtService.generateToken(1L)).isNotEqualTo(jwtService.generateToken(1L));
    }

    // ---------- Expiración ----------

    @Test
    @DisplayName("T-SEC-007: 16 minutos después el token está vencido")
    void tokenVencido_lanzaExpiredJwtException() {
        String token = jwtService.generateToken(1L);
        JwtService dieciseisMinutosDespues = servicio(ISSUER, SECRET_32, AHORA.plus(Duration.ofMinutes(16)));

        assertThatThrownBy(() -> dieciseisMinutosDespues.validateAndGetUserId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("14 minutos después el token sigue vigente")
    void tokenAntesDeVencer_esValido() {
        String token = jwtService.generateToken(1L);
        JwtService catorceMinutosDespues = servicio(ISSUER, SECRET_32, AHORA.plus(Duration.ofMinutes(14)));

        assertThat(catorceMinutosDespues.validateAndGetUserId(token)).isEqualTo(1L);
    }

    // ---------- Manipulación e integridad ----------

    @Test
    @DisplayName("T-SEC-008: cambiar un carácter de la firma -> rechazo")
    void firmaAlterada_lanzaJwtException() {
        String token = jwtService.generateToken(1L);
        // Se cambia un carácter del MEDIO de la firma. El último no sirve: en Base64URL sus bits
        // finales son relleno y cambiarlo puede no alterar los bytes de la firma
        int posicion = token.lastIndexOf('.') + 10;
        char nuevo = token.charAt(posicion) == 'A' ? 'B' : 'A';
        String alterado = token.substring(0, posicion) + nuevo + token.substring(posicion + 1);

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(alterado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Andrea cambia sub=1 por sub=2 en el payload para hacerse pasar por Carlos -> rechazo")
    void payloadAlterado_lanzaJwtException() {
        String token = jwtService.generateToken(1L);
        String[] partes = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String payloadFalso = payload.replace("\"sub\":\"1\"", "\"sub\":\"2\"");
        String falso = partes[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payloadFalso.getBytes(StandardCharsets.UTF_8))
                + "." + partes[2];

        assertThat(payloadFalso).contains("\"sub\":\"2\"");
        assertThatThrownBy(() -> jwtService.validateAndGetUserId(falso))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Token firmado con otra clave -> rechazo")
    void otraClave_lanzaJwtException() {
        String token = servicio(ISSUER, OTRO_SECRET_32, AHORA).generateToken(1L);

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("UC-SEC-09: token emitido por otro issuer -> rechazo")
    void otroIssuer_lanzaJwtException() {
        String token = servicio("otro-emisor", SECRET_32, AHORA).generateToken(1L);

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("§12.2: token sin firma (alg=none) -> rechazo")
    void tokenSinFirma_lanzaJwtException() {
        String sinFirma = Jwts.builder()
                .subject("1").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(AHORA)).expiration(Date.from(AHORA.plus(TTL)))
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(sinFirma))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("FR-SEC-AUTH-006: algoritmo inesperado (HS512 con la misma clave) -> rechazo")
    void algoritmoDistintoDeHs256_esRechazado() {
        JwtService servicio64 = servicio(ISSUER, SECRET_64, AHORA);
        String hs512 = Jwts.builder()
                .subject("1").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(AHORA)).expiration(Date.from(AHORA.plus(TTL)))
                .signWith(clave(SECRET_64, "HmacSHA512"), Jwts.SIG.HS512)
                .compact();

        // JJWT puede rechazarlo por la clave o JwtService por el header alg: en ambos casos el filtro responde 401
        assertThatThrownBy(() -> servicio64.validateAndGetUserId(hs512))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    // ---------- Claims obligatorios ----------

    @Test
    @DisplayName("§14.4: token firmado pero sin exp -> rechazo (JJWT no lo exige solo)")
    void sinExp_esRechazado() {
        String token = Jwts.builder()
                .subject("1").issuer(ISSUER).id(UUID.randomUUID().toString()).issuedAt(Date.from(AHORA))
                .signWith(clave(SECRET_32, "HmacSHA256"), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("exp");
    }

    @Test
    @DisplayName("§12.1: token firmado pero sin sub -> rechazo")
    void sinSub_esRechazado() {
        String token = Jwts.builder()
                .issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(AHORA)).expiration(Date.from(AHORA.plus(TTL)))
                .signWith(clave(SECRET_32, "HmacSHA256"), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("sub");
    }

    @Test
    @DisplayName("§12.1: sub que no es un id numérico -> rechazo")
    void subNoNumerico_esRechazado() {
        String token = Jwts.builder()
                .subject("andrea@pulsepass.com").issuer(ISSUER).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(AHORA)).expiration(Date.from(AHORA.plus(TTL)))
                .signWith(clave(SECRET_32, "HmacSHA256"), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("§12.1: token firmado pero sin jti -> rechazo")
    void sinJti_esRechazado() {
        String token = Jwts.builder()
                .subject("1").issuer(ISSUER)
                .issuedAt(Date.from(AHORA)).expiration(Date.from(AHORA.plus(TTL)))
                .signWith(clave(SECRET_32, "HmacSHA256"), Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateAndGetUserId(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("jti");
    }

    // ---------- Entradas inválidas (NFR-SEC-002: nunca un 500) ----------

    @Test
    @DisplayName("Token vacío o nulo -> IllegalArgumentException")
    void tokenVacio_lanzaIllegalArgument() {
        assertThatThrownBy(() -> jwtService.validateAndGetUserId(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> jwtService.validateAndGetUserId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Texto que no es un JWT -> rechazo")
    void textoBasura_esRechazado() {
        assertThatThrownBy(() -> jwtService.validateAndGetUserId("esto.no.es-un-jwt"))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    // ---------- Configuración de la clave (§12.2: la app no arranca) ----------

    @Test
    @DisplayName("§12.2: clave ausente -> no arranca")
    void claveAusente_fallaAlCrear() {
        assertThatThrownBy(() -> servicio(ISSUER, null, AHORA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEPASS_JWT_SECRET_BASE64");
    }

    @Test
    @DisplayName("§12.2: clave de 16 bytes (débil) -> no arranca")
    void claveCorta_fallaAlCrear() {
        String dieciseisBytes = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> servicio(ISSUER, dieciseisBytes, AHORA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("§12.2: clave que no es Base64 -> no arranca")
    void claveNoBase64_fallaAlCrear() {
        assertThatThrownBy(() -> servicio(ISSUER, "%%% no es base64 %%%", AHORA))
                .isInstanceOf(IllegalStateException.class);
    }
}
