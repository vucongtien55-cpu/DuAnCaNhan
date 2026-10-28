package org.example.ridehailing.ride_hailing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

//@SpringBootTest
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
class RideHailingApplicationTests {
    public static void main(String[] args) {
        SpringApplication.run(RideHailingApplication.class, args);
    }

}