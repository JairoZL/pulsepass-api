package com.pulsepass.exception;

// Login fallido. Un solo mensaje para "no existe", "contraseña incorrecta", "sin credencial"
// e "inactivo": no se revela cuál fue (PRD Seguridad FR-SEC-AUTH-003). GlobalExceptionHandler -> 401
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
