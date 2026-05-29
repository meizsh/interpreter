package com.interpreter.aibackend.service;

import com.interpreter.aibackend.entity.Session;
import com.interpreter.aibackend.entity.TermHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务
 * 使用内存缓存存储会话数据
 */
@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    // 使用 ConcurrentHashMap 存储会话
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 创建新会话
     */
    public Session createSession(String audioFileName, String audioPath, Long audioSize, String interpretationDirection) {
        String sessionId = UUID.randomUUID().toString();

        Session session = new Session();
        session.setSessionId(sessionId);
        session.setAudioFileName(audioFileName);
        session.setAudioPath(audioPath);
        session.setAudioSize(audioSize);
        session.setInterpretationDirection(interpretationDirection);
        session.setStatus("uploaded");
        session.setCreatedAt(LocalDateTime.now());

        sessions.put(sessionId, session);
        logger.info("✓ 创建新会话: {}", sessionId);
        return session;
    }

    /**
     * 获取会话
     */
    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            logger.warn("⚠️ 会话不存在: {}", sessionId);
        }
        return session;
    }

    /**
     * 更新会话（原文和翻译）
     */
    @SuppressWarnings("unchecked")
    public void updateSessionWithTranslation(String sessionId, String originalText, String standardTranslation, Object termHints) {
        Session session = getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在: " + sessionId);
        }

        session.setOriginalText(originalText);
        session.setStandardTranslation(standardTranslation);
        session.setTermHints((List<TermHint>) termHints);
        session.setStatus("completed");
        session.setProcessedAt(LocalDateTime.now());

        logger.info("✓ 会话数据已更新: {}", sessionId);
    }

    /**
     * 更新学生翻译
     */
    public void updateStudentTranslation(String sessionId, String studentTranslation) {
        Session session = getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在: " + sessionId);
        }

        session.setStudentTranslation(studentTranslation);
        logger.info("✓ 学生翻译已更新: {}", sessionId);
    }

    /**
     * 更新诊断报告
     */
    public void updateDiagnosisReport(String sessionId, Object diagnosisReport) {
        Session session = getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在: " + sessionId);
        }

        session.setDiagnosisReport((com.interpreter.aibackend.entity.DiagnosisReport) diagnosisReport);
        session.setStatus("graded");
        session.setGradedAt(LocalDateTime.now());

        logger.info("✓ 诊断报告已更新: {}", sessionId);
    }

    /**
     * 设置错误状态
     */
    public void setError(String sessionId, String errorMessage) {
        Session session = getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在: " + sessionId);
        }

        session.setStatus("error");
        session.setErrorMessage(errorMessage);

        logger.error("❌ 会话处理失败 {}: {}", sessionId, errorMessage);
    }

    /**
     * 删除会话
     */
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        logger.info("✓ 会话已删除: {}", sessionId);
    }

    /**
     * 获取所有会话（测试用）
     */
    public Collection<Session> getAllSessions() {
        return sessions.values();
    }

    /**
     * 获取会话数量（测试用）
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 清空所有会话（测试用）
     */
    public void clearAllSessions() {
        sessions.clear();
        logger.warn("⚠️ 所有会话已清空");
    }
}
