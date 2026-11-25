package com.pulsepass.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;

// Clave compuesta de user_roles: PRIMARY KEY (user_id, role) en V4.
// JPA exige que una clave compuesta sea Serializable e implemente equals y hashCode,
// porque la usa para identificar la fila (caché de primer nivel, findById).
@Embeddable
@Getter
public class UserRoleId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Role role;

    protected UserRoleId() { }

    public UserRoleId(Long userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    // Dos claves son iguales si tienen el mismo usuario y el mismo rol
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserRoleId other)) return false;
        return Objects.equals(userId, other.userId) && role == other.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, role);
    }
}
