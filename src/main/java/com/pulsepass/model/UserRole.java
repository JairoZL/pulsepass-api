package com.pulsepass.model;

import jakarta.persistence.*;
import lombok.Getter;

// PRD Seguridad §9.1: rol de un usuario (N:1 con User), tabla user_roles (V4).
@Entity
@Table(name = "user_roles")
@Getter
public class UserRole {

    // La PK es la pareja (user_id, role), ver UserRoleId
    @EmbeddedId
    private UserRoleId id;

    // @MapsId("userId"): el campo userId de la clave se toma de esta relación
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    protected UserRole() { }

    // El usuario ya debe estar guardado (tener id) para armar la clave
    public UserRole(User user, Role role) {
        this.id = new UserRoleId(user.getId(), role);
        this.user = user;
    }

    // Atajo para no escribir userRole.getId().getRole() en la capa de seguridad
    public Role getRole() {
        return id.getRole();
    }
}
