package com.interpreter.aibackend.controller;

import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.entity.Session;
import com.interpreter.aibackend.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会话查询接口。
 * 前端通过这些接口轮询处理状态、读取术语卡片和诊断报告。
 */
@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SessionController {
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService;

    /**
     * 获取素材分析结果：原文、参考译文、术语提示。
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
            response.put("interpretation_direction", session.getInterpretationDirection());
            response.put("original_text", session.getOriginalText());
            response.put("standard_translation", session.getStandardTranslation());
            response.put("term_hints", session.getTermHints());

            logger.info("读取会话素材分析结果：{}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("读取会话素材分析结果失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取学生口译诊断报告。
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
            response.put("interpretation_direction", session.getInterpretationDirection());
            response.put("original_text", session.getOriginalText());
            response.put("standard_translation", session.getStandardTranslation());
            response.put("student_translation", session.getStudentTranslation());
            response.put("diagnosis_report", session.getDiagnosisReport());

            logger.info("读取学生口译诊断报告：{}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("读取学生口译诊断报告失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 获取当前会话处理状态。
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
            response.put("interpretation_direction", session.getInterpretationDirection());
            if ("error".equals(session.getStatus())) {
                response.put("error_message", session.getErrorMessage());
            }

            logger.info("读取会话状态：{} -> {}", sessionId, session.getStatus());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("读取会话状态失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 删除会话。
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
        try {
            sessionService.deleteSession(sessionId);

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");

            logger.info("删除会话：{}", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除会话失败", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 后端健康检查。
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        JSONObject response = new JSONObject();
        response.put("code", 0);
        response.put("message", "OK");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    private JSONObject createErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("code", -1);
        response.put("message", message);
        return response;
    }
}
