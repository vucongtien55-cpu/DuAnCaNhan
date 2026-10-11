package org.example.ridehailing.banhangonline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BanHangOnlineApplication {

    public static void main(String[] args) {
        SpringApplication.run(BanHangOnlineApplication.class, args);
    }

}
