package com.pulsepass.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

// PRD Seguridad §9.1: credencial 1:1 con User, en tabla aparte (user_credentials, V4).
// Sin @ToString: el hash nunca debe terminar en un log (SEC-003, NFR-SEC-001).
@Entity
@Table(name = "user_credentials")
@Getter
public class UserCredential {

    // La PK no se genera: es el mismo id del usuario. @MapsId la copia desde "user".
    @Id
    private Long userId;

    // @MapsId: la columna user_id es a la vez PK y FK hacia users
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // Solo el hash BCrypt, nunca la contraseña en texto plano (SEC-003)
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    protected UserCredential() { }

    // Recibe el hash ya calculado: la entidad no sabe hashear, eso lo hace el PasswordEncoder
    public UserCredential(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = LocalDateTime.now();
    }
}
