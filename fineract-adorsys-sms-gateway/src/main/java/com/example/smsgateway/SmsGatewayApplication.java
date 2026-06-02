package com.example.smsgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.smsgateway")
@EnableScheduling
public class SmsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsGatewayApplication.class, args);
    }
}
