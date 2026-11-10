package com.pulsepass.model;


import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "venues")
@Getter
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false,length = 150)
    private String name;

    @Column(name = "city",nullable = false,length = 100)
    private String city;

    @Column(name = "address",nullable = false,length = 255)
    private String address;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "active",nullable = false)
    private Boolean active = true;

    protected Venue() { }

    public Venue(String code, String name, String city, String address, Integer capacity) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.capacity = capacity;
    }
}
