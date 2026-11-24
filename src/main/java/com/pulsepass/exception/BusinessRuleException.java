package com.pulsepass.exception;

// El recurso existe pero la operación no es válida, p. ej. publicar un evento que no está en DRAFT (se traduce a 409)
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
