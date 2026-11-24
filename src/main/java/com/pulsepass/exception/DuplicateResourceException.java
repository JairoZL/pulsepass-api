package com.pulsepass.exception;

// Conflicto de unicidad, p. ej. username o eventCode repetido (se traduce a 409)
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
