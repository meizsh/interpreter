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
 * 浼氳瘽绠＄悊 Controller
 */
@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class SessionController {
    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    @Autowired
    private SessionService sessionService;

    /**
     * 鑾峰彇浼氳瘽鍏冩暟鎹紙鍘熸枃銆佺炕璇戙€佹湳璇級
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
            response.put("interpretation_direction", session.getInterpretationDirection());
            response.put("original_text", session.getOriginalText());
            response.put("standard_translation", session.getStandardTranslation());
            response.put("term_hints", session.getTermHints());

            logger.info("鉁?杩斿洖浼氳瘽鍏冩暟鎹? {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get session metadata", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 鑾峰彇璇婃柇鎶ュ憡
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
                return ResponseEntity.badRequest().body(createErrorResponse("璇婃柇鎶ュ憡灏氭湭鐢熸垚"));
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

            logger.info("鉁?杩斿洖璇婃柇鎶ュ憡: {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("鉂?鑾峰彇璇婃柇鎶ュ憡澶辫触", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 鑾峰彇浼氳瘽鐘舵€?
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
            response.put("interpretation_direction", session.getInterpretationDirection());

            if (session.getStatus().equals("error")) {
                response.put("error_message", session.getErrorMessage());
            }

            logger.info("鉁?杩斿洖浼氳瘽鐘舵€? {} -> {}", sessionId, session.getStatus());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Failed to get session status", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 鍒犻櫎浼氳瘽
     * DELETE /api/session/{sessionId}
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
        try {
            sessionService.deleteSession(sessionId);

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");

            logger.info("鉁?浼氳瘽宸插垹闄? {}", sessionId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("鉂?鍒犻櫎浼氳瘽澶辫触", e);
            return ResponseEntity.internalServerError().body(createErrorResponse(e.getMessage()));
        }
    }

    /**
     * 鍋ュ悍妫€鏌?
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
     * 鍒涘缓閿欒鍝嶅簲
     */
    private JSONObject createErrorResponse(String message) {
        JSONObject response = new JSONObject();
        response.put("code", -1);
        response.put("message", message);
        return response;
    }
}
