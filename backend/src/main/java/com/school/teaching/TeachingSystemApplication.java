package com.school.teaching;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@MapperScan("com.school.teaching.mapper")
@EnableScheduling
@EnableAsync
@EnableCaching
public class TeachingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeachingSystemApplication.class, args);
        log.info("Teaching System started!");
    }
}
