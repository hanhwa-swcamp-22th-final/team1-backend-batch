package com.conk.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 서비스의 진입점이다.
 */
@EnableScheduling
@EnableFeignClients(basePackages = "com.conk.batch.billing.client")
@SpringBootApplication
public class Team1BackendBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(Team1BackendBatchApplication.class, args);
    }
}
