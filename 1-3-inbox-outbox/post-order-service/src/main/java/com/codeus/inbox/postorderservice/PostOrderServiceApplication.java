package com.codeus.inbox.postorderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PostOrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostOrderServiceApplication.class, args);
    }

}
