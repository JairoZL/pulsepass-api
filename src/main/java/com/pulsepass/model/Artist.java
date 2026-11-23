package com.pulsepass.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "artists")
@Getter
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, unique = true, length = 80)
    private String stageName;

    @Column(name = "country", nullable = false, length = 80)
    private String country;

    @Column(name = "genre", nullable = false, length = 50)
    private String genre;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    protected Artist() { }

    public Artist(String stageName, String country, String genre) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
    }
}
