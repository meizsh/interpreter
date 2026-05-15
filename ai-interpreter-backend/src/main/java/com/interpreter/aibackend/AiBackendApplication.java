package com.interpreter.aibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiBackendApplication.class, args);
        System.out.println("🚀 AI 口译辅助后端启动成功！");
    }
}