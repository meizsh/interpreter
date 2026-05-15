package com.interpreter.aibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AiBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiBackendApplication.class, args);
        System.out.println("🚀 AI 口译辅助后端启动成功！");
        System.out.println("📡 服务地址: http://localhost:8080");
        System.out.println("🎙️ API 文档:");
        System.out.println("  - 上传音频: POST /api/audio/upload");
        System.out.println("  - 获取元数据: GET /api/session/{sessionId}/metadata");
        System.out.println("  - 获取报告: GET /api/session/{sessionId}/report");
    }
}