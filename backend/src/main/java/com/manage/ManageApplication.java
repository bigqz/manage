package com.manage;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@MapperScan("com.manage.mapper")
@RestController
public class ManageApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManageApplication.class, args);
    }

    @GetMapping("/")
    public String root() {
        return "OK - Manage Backend";
    }

    @GetMapping("/health")
    public String health() {
        return "OK - Manage Backend Running";
    }
}
