package com.pulsepass.exception;

// El recurso solicitado no existe (se traduce a 404 en GlobalExceptionHandler)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
