package com.dmi.canteen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartCanteenApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartCanteenApplication.class, args);
    }
}
