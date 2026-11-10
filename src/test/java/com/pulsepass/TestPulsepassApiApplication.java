package com.pulsepass;

import org.springframework.boot.SpringApplication;

public class TestPulsepassApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(PulsepassApiApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
