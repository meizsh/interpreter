package com.interpreter.aibackend.controller;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 绯荤粺鍋ュ悍妫€鏌?Controller
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class HealthController {

    /**
     * 鍋ュ悍妫€鏌?
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
     * 鑾峰彇 API 淇℃伅
     * GET /api/info
     */
    @GetMapping("/info")
    public ResponseEntity<?> getInfo() {
        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("name", "AI 鍙ｈ瘧杈呭姪绯荤粺");
        response.put("version", "1.0.0");
        response.put("description", "鑷€傚簲浜烘満鍗忎綔鍙ｈ瘧璁粌娌欑洅");
        
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
