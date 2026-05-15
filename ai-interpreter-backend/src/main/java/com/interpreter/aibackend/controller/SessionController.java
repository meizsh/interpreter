package com.interpreter.aibackend.controller;

import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.entity.Session;
import com.interpreter.aibackend.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 会话管理 Controller
 */
@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "http://localhost:5173")
public class SessionController {
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService;

    /**
     * 获取会话元数据（原文、翻译、术语）
     * GET /api/session/{sessionId}/metadata
     */
    @GetMapping("/{sessionId}/metadata")
    public ResponseEntity<?> getSessionMetadata(@PathVariable String sessionId) {
        try {
            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");
            response.put("session_id", session.getSessionId());
            response.put("status", session.getStatus());
            response.put("original_text", session.getOriginalText());
            response.put("standard_translation", session.getStandardTranslation());
            response.put("term_hints", session.getTermHints());

            logger.info("✓ 返回会话元数据: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取元数据失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取诊断报告
     * GET /api/session/{sessionId}/report
     */
    @GetMapping("/{sessionId}/report")
    public ResponseEntity<?> getDiagnosisReport(@PathVariable String sessionId) {
        try {
            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            if (session.getDiagnosisReport() == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("诊断报告尚未生成"));
            }

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");
            response.put("session_id", session.getSessionId());
            response.put("status", session.getStatus());
            response.put("original_text", session.getOriginalText());
            response.put("standard_translation", session.getStandardTranslation());
            response.put("student_translation", session.getStudentTranslation());
            response.put("diagnosis_report", session.getDiagnosisReport());

            logger.info("✓ 返回诊断报告: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取诊断报告失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取会话状态
     * GET /api/session/{sessionId}/status
     */
    @GetMapping("/{sessionId}/status")
    public ResponseEntity<?> getSessionStatus(@PathVariable String sessionId) {
        try {
            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("session_id", session.getSessionId());
            response.put("status", session.getStatus());

            if (session.getStatus().equals("error")) {
                response.put("error_message", session.getErrorMessage());
            }

            logger.info("✓ 返回会话状态: {} -> {}", sessionId, session.getStatus());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 获取状态失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 删除会话
     * DELETE /api/session/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
        try {
            sessionService.deleteSession(sessionId);

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");

            logger.info("✓ 会话已删除: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 删除会话失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 健康检查
     * GET /api/session/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("message", "OK");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 创建错误响应
     */
    private JSONObject createErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("code", -1);
        response.put("message", message);
        return response;
    }
}
