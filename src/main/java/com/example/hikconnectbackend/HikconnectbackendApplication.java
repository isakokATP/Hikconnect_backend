package com.example.hikconnectbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class HikconnectbackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HikconnectbackendApplication.class, args);
    }

}
