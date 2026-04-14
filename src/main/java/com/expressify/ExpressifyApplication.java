package com.expressify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ExpressifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpressifyApplication.class, args);
    }
}
