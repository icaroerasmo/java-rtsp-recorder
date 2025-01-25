package com.icaroerasmo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class JavaRtsRecorderpApplication {
    public static void main(String[] args) {
        SpringApplication.run(JavaRtsRecorderpApplication.class, args);
    }
}
