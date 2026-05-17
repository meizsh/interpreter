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
import java.util.Map;

@RestController
@RequestMapping("/api/audio")
@CrossOrigin(origins = "http://localhost:5173") // 保持您原有的前端跨域允许
public class AudioController {
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private AudioProcessingService audioProcessingService;

    @Autowired
    private SessionService sessionService;

    /**
     * 1. 演讲原音上传（触发 AI 自动洗出：原文 + 标答 + 术语轴）
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", -1, "message", "文件为空"));
            }

            String filename = file.getOriginalFilename();
            // 保存并创建会话
            Session session = audioProcessingService.saveAudioFile(file.getBytes(), filename);
            logger.info("✓ 原音已上传，激活 Session ID: {}", session.getSessionId());

            // 异步触发后台处理链路 (ASR + 翻译 + 术语提取)
            audioProcessingService.processAudioAsync(session.getSessionId());

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("session_id", session.getSessionId());
            response.put("status", "processing");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", -1, "message", e.getMessage()));
        }
    }

    /**
     * 2. 接收学生口译录音并【同步等待评卷完成】（省去前端复杂的多次轮询）
     */
    @PostMapping("/{sessionId}/student-audio")
    public ResponseEntity<?> uploadStudentAudio(
            @PathVariable String sessionId,
            @RequestParam("studentAudio") MultipartFile file) {
        try {
            logger.info("🎤 收到学生口译录音，开始阅卷: {}", sessionId);

            // 1. 保存并识别学生口译内容
            audioProcessingService.processStudentAudio(sessionId, file.getBytes(), file.getOriginalFilename());

            // 2. 触发大模型诊断系统（进行原文、标准答案、学生译文的三方比对）
            audioProcessingService.diagnoseStudentTranslation(sessionId);

            // 3. 阻塞等待大模型阅卷诊断报告生成（最多等8秒，答辩和日常训练最稳妥的联调技巧）
            Session finalSession = null;
            int retry = 0;
            while (retry < 8) {
                Thread.sleep(1000);
                finalSession = sessionService.getSession(sessionId);
                if (finalSession != null && finalSession.getDiagnosisReport() != null) {
                    break;
                }
                retry++;
            }

            // 直接将热腾腾的、比对好的多维报告数据一次性打包返给前端
            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("status", "COMPLETED");
            response.put("score", finalSession.getDiagnosisReport().getScore());
            response.put("accuracyAnalysis", finalSession.getDiagnosisReport().getAccuracyAnalysis());
            response.put("grammarStyle", finalSession.getDiagnosisReport().getGrammarStyle());
            response.put("suggestions", finalSession.getDiagnosisReport().getSuggestions());
            response.put("originalText", finalSession.getOriginalText());
            response.put("standardTranslation", finalSession.getStandardTranslation());
            response.put("studentTranslation", finalSession.getStudentTranslation());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("❌ 评卷失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("code", -1, "message", e.getMessage()));
        }
    }
}