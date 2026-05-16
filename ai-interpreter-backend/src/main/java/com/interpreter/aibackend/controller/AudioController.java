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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 音频处理 Controller
 */
@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "http://localhost:5173")
public class AudioController {
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private AudioProcessingService audioProcessingService;

    @Autowired
    private SessionService sessionService;

    /**
     * 上传音频文件
     * POST /api/audio/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("📤 收到音频上传请求: {}", file.getOriginalFilename());

            // 验证文件
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("文件为空"));
            }

            String filename = file.getOriginalFilename();
            if (!isValidAudioFormat(filename)) {
                return ResponseEntity.badRequest().body(createErrorResponse("不支持的音频格式，仅支持 MP3、WAV、M4A"));
            }

            // 保存文件并创建会话
            Session session = audioProcessingService.saveAudioFile(
                    file.getBytes(),
                    filename
            );

            logger.info("✓ 音频已上传，Session ID: {}", session.getSessionId());

            // 异步处理音频
            audioProcessingService.processAudioAsync(session.getSessionId());

            // 返回响应
            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");
            response.put("session_id", session.getSessionId());
            response.put("status", "processing");
            response.put("tip", "🎤 AI 正在后台分析音频并自动翻译，请稍候...");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("上传失败: " + e.getMessage()));
        }
    }

    /**
     * 上传学生口译录音
     * POST /api/audio/{sessionId}/student-audio
     */
    @PostMapping("/{sessionId}/student-audio")
    public ResponseEntity<?> uploadStudentAudio(
            @PathVariable String sessionId,
            @RequestParam("file") MultipartFile file) {
        try {
            logger.info("🎤 收到学生口译录音: {}", sessionId);

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            // 处理学生录音
            audioProcessingService.processStudentAudio(sessionId, file.getBytes(), file.getOriginalFilename());

            // 异步诊断学生翻译
            audioProcessingService.diagnoseStudentTranslation(sessionId);

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("message", "success");
            response.put("status", "grading");
            response.put("tip", "✨ AI 正在评阅你的口译...");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 学生录音处理失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("处理失败: " + e.getMessage()));
        }
    }

    /**
     * 验证音频格式
     */
    private boolean isValidAudioFormat(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".m4a");
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
