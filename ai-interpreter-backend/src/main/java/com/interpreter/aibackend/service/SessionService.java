package com.interpreter.aibackend.service;

import com.interpreter.aibackend.entity.DiagnosisReport;
import com.interpreter.aibackend.entity.Session;
import com.interpreter.aibackend.entity.TermHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话管理服务。
 * 当前使用内存 Map 存储会话，适合本地原型；生产环境应替换为数据库或持久化缓存。
 */
@Service
public class SessionService {
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /**
     * 创建一次新的口译训练会话。
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
        logger.info("创建会话：{}", sessionId);
        return session;
    }

    /**
     * 根据 ID 获取会话。
     */
    public Session getSession(String sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            logger.warn("会话不存在：{}", sessionId);
        }
        return session;
    }

    /**
     * 保存素材 ASR、参考译文和术语提示。
     */
    @SuppressWarnings("unchecked")
    public void updateSessionWithTranslation(String sessionId, String originalText, String standardTranslation, Object termHints) {
        Session session = getRequiredSession(sessionId);

        session.setOriginalText(originalText);
        session.setStandardTranslation(standardTranslation);
        session.setTermHints((List<TermHint>) termHints);
        session.setStatus("completed");
        session.setProcessedAt(LocalDateTime.now());

        logger.info("素材分析结果已更新：{}", sessionId);
    }

    /**
     * 保存学生口译 ASR 文本。
     */
    public void updateStudentTranslation(String sessionId, String studentTranslation) {
        Session session = getRequiredSession(sessionId);

        session.setStudentTranslation(studentTranslation);
        logger.info("学生口译文本已更新：{}", sessionId);
    }

    /**
     * 保存学生口译诊断报告。
     */
    public void updateDiagnosisReport(String sessionId, Object diagnosisReport) {
        Session session = getRequiredSession(sessionId);

        session.setDiagnosisReport((DiagnosisReport) diagnosisReport);
        session.setStatus("graded");
        session.setGradedAt(LocalDateTime.now());

        logger.info("诊断报告已更新：{}", sessionId);
    }

    /**
     * 标记会话处理失败。
     */
    public void setError(String sessionId, String errorMessage) {
        Session session = getRequiredSession(sessionId);

        session.setStatus("error");
        session.setErrorMessage(errorMessage);

        logger.error("会话处理失败 {}：{}", sessionId, errorMessage);
    }

    /**
     * 删除会话。
     */
    public void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        logger.info("会话已删除：{}", sessionId);
    }

    public Collection<Session> getAllSessions() {
        return sessions.values();
    }

    public int getSessionCount() {
        return sessions.size();
    }

    public void clearAllSessions() {
        sessions.clear();
        logger.warn("所有会话已清空");
    }

    private Session getRequiredSession(String sessionId) {
        Session session = getSession(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在：" + sessionId);
        }
        return session;
    }
}
