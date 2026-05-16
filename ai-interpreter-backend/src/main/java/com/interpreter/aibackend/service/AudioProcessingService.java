package com.interpreter.aibackend.service;

import com.interpreter.aibackend.entity.Session;
import com.interpreter.aibackend.entity.TermHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 音频处理服务
 * 协调整个处理流程：上传 -> ASR -> 翻译 -> 术语提取
 */
@Service
public class AudioProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingService.class);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private BaiduAsrService baiduAsrService;

    @Autowired
    private BaiduLlmService baiduLlmService;

    @Value("${upload.dir}")
    private String uploadDir;

    /**
     * 保存上传的音频文件
     */
    public Session saveAudioFile(byte[] fileContent, String originalFileName) throws Exception {
        // 创建上传目录
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        // 生成唯一文件名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePath = uploadDir + "/" + timestamp + "_" + originalFileName;

        // 保存文件
        Files.write(Paths.get(filePath), fileContent);
        logger.info("✓ 音频文件已保存: {}", filePath);

        // 创建会话
        Session session = sessionService.createSession(originalFileName, filePath, (long) fileContent.length);
        return session;
    }

    /**
     * 异步处理音频（ASR + 翻译 + 术语提取）
     * 这是核心的数据处理流程
     */
    @Async
    public void processAudioAsync(String sessionId) {
        try {
            logger.info("🔄 开始异步处理音频: {}", sessionId);

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                logger.error("❌ 会话不存在: {}", sessionId);
                return;
            }

            // Step 1: 调用百度 ASR 进行语音识别
            logger.info("📊 Step 1/3: 语音识别中...");
            String originalText = baiduAsrService.transcribeAudio(session.getAudioPath());

            // Step 2: 调用百度 LLM 进行翻译
            logger.info("📊 Step 2/3: 翻译中...");
            String standardTranslation = baiduLlmService.translateText(originalText);

            // Step 3: 调用百度 LLM 提取术语
            logger.info("📊 Step 3/3: 术语提取中...");
            List<TermHint> termHints = baiduLlmService.extractTerms(originalText);

            // 更新会话
            sessionService.updateSessionWithTranslation(sessionId, originalText, standardTranslation, termHints);

            logger.info("✓ 音频处理完成！");
            logger.info("  原文长度: {} 字", originalText.length());
            logger.info("  翻译长度: {} 字", standardTranslation.length());
            logger.info("  术语数量: {} 个", termHints.size());

        } catch (Exception e) {
            logger.error("❌ 音频处理失败", e);
            sessionService.setError(sessionId, e.getMessage());
        }
    }

    /**
     * 处理学生的口译录音
     */
    public void processStudentAudio(String sessionId, byte[] audioContent, String originalFileName) throws Exception {
        logger.info("🎤 处理学生口译录音: {}", sessionId);

        // 保存学生录音文件，保留原始扩展名
        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFileName);
        if (extension.isEmpty()) {
            extension = "wav";
        }
        String studentAudioPath = uploadDir + "/" + timestamp + "_student." + extension;
        Files.write(Paths.get(studentAudioPath), audioContent);

        // 调用 ASR 识别学生口译
        String studentTranslation = baiduAsrService.transcribeAudio(studentAudioPath);
        logger.info("✓ 学生口译识别完成: {}", studentTranslation);

        // 更新会话
        sessionService.updateStudentTranslation(sessionId, studentTranslation);
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase();
    }

    /**
     * 诊断学生的口译
     */
    @Async
    public void diagnoseStudentTranslation(String sessionId) {
        try {
            logger.info("📊 开始诊断学生口译: {}", sessionId);

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                logger.error("❌ 会话不存在: {}", sessionId);
                return;
            }

            if (session.getOriginalText() == null || session.getStudentTranslation() == null) {
                throw new RuntimeException("缺少必要的数据（原文或学生翻译）");
            }

            // 调用 LLM 进行诊断
            var report = baiduLlmService.diagnoseTranslation(
                    session.getOriginalText(),
                    session.getStandardTranslation(),
                    session.getStudentTranslation()
            );

            // 更新会话
            sessionService.updateDiagnosisReport(sessionId, report);

            logger.info("✓ 诊断完成！评分: {}", report.getScore());

        } catch (Exception e) {
            logger.error("❌ 诊断失败", e);
            sessionService.setError(sessionId, e.getMessage());
        }
    }

    /**
     * 清理过期的音频文件
     */
    public void cleanupOldAudioFiles(long maxAgeMillis) {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists() || !uploadPath.isDirectory()) {
            return;
        }

        File[] files = uploadPath.listFiles();
        if (files == null) return;

        long now = System.currentTimeMillis();
        for (File file : files) {
            if (file.isFile() && (now - file.lastModified()) > maxAgeMillis) {
                file.delete();
                logger.info("✓ 已删除过期文件: {}", file.getName());
            }
        }
    }
}
