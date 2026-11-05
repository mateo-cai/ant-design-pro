package com.gdstc.beszel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeszelAlerterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeszelAlerterApplication.class, args);
    }
}
