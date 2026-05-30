package com.interpreter.aibackend.controller;

import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.entity.Session;
import com.interpreter.aibackend.service.AudioProcessingService;
import com.interpreter.aibackend.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 音频上传接口。
 * 包含素材音频上传和学生口译录音上传两个入口。
 */
@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AudioController {
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private AudioProcessingService audioProcessingService;

    @Autowired
    private SessionService sessionService;

    /**
     * 上传口译素材音频，并启动异步处理流程。
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "direction", defaultValue = "en-zh") String direction) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", -1, "message", "音频文件不能为空"));
            }

            String filename = file.getOriginalFilename();
            Session session = audioProcessingService.saveAudioFile(file.getBytes(), filename, direction);
            logger.info("素材音频上传成功，Session ID: {}", session.getSessionId());

            audioProcessingService.processAudioAsync(session.getSessionId());

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("session_id", session.getSessionId());
            response.put("status", "processing");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("素材音频上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", -1, "message", e.getMessage()));
        }
    }

    /**
     * 上传学生口译录音，并启动异步诊断流程。
     */
    @PostMapping("/{sessionId}/student-audio")
    public ResponseEntity<?> uploadStudentAudio(
            @PathVariable String sessionId,
            @RequestParam("studentAudio") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", -1, "message", "学生口译录音不能为空"));
            }

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", -1, "message", "会话不存在"));
            }

            audioProcessingService.processStudentAudioAndDiagnoseAsync(
                    sessionId,
                    file.getBytes(),
                    file.getOriginalFilename()
            );

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("status", "processing");
            response.put("message", "学生口译录音已接收，正在生成诊断报告");
            response.put("session_id", sessionId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            logger.error("学生口译录音上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", -1, "message", e.getMessage()));
        }
    }
}
