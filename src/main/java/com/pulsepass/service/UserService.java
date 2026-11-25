package com.pulsepass.service;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;

// Contrato: PRD Servicios §18
// Seguridad (PRD Seguridad §11.1): un usuario solo puede consultarse a sí mismo; ADMIN a cualquiera.
// El registro no lleva anotación: es público (FR-SEC-USR-01) y AuthService lo invoca.
public interface UserService {

    UserResponse register(RegisterUserRequest request);

    // FR-SEC-USR-02
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelfEmail(#email)")
    UserResponse findByEmail(String email);

    // FR-SEC-USR-03
    @PreAuthorize("hasRole('ADMIN') or @authz.isSelfUsername(#username)")
    UserResponse findByUsername(String username);
}
