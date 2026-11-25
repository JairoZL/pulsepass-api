package com.pulsepass.model;

// PRD Seguridad §5.1: tres roles. Se guardan sin prefijo (USER, STAFF, ADMIN);
// el prefijo "ROLE_" lo agrega la capa de seguridad al construir las authorities.
public enum Role {
    USER,
    STAFF,
    ADMIN
}
