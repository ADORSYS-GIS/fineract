package com.example.smsgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "com.example.smsgateway")
public class SmsGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmsGatewayApplication.class, args);
    }
}
