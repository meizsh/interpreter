package com.interpreter.aibackend.controller;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统健康检查 Controller
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class HealthController {

    /**
     * 健康检查
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("message", "OK");
        response.put("status", "running");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取 API 信息
     * GET /api/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("name", "AI 口译辅助系统");
        response.put("version", "1.0.0");
        response.put("description", "自适应人机协作口译训练沙盒");
        
        JSONObject apis = new JSONObject();
        apis.put("upload_audio", "POST /api/audio/upload");
        apis.put("get_metadata", "GET /api/session/{sessionId}/metadata");
        apis.put("get_report", "GET /api/session/{sessionId}/report");
        apis.put("get_status", "GET /api/session/{sessionId}/status");
        apis.put("delete_session", "DELETE /api/session/{sessionId}");
        apis.put("upload_student_audio", "POST /api/audio/{sessionId}/student-audio");
        response.put("apis", apis);
        
        return ResponseEntity.ok(response);
    }
}
