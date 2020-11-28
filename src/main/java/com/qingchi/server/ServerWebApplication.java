package com.qingchi.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EntityScan("com.qingchi")
@EnableJpaRepositories("com.qingchi")
@ComponentScan(value="com.qingchi")
@EnableTransactionManagement
@EnableCaching
public class ServerWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerWebApplication.class, args);
    }
}
