package com.pulsepass.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "users")
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    protected User() { }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
    }
}
