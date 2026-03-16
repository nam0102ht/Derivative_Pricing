package com.quantpricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class QuantPricingApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuantPricingApplication.class, args);
    }
}
