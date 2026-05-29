package com.interpreter.aibackend.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 口译会话实体
 */
public class Session {
    private String sessionId;
    private String audioFileName;
    private String audioPath;
    private Long audioSize;
    private String interpretationDirection;
    
    // 原文和翻译
    private String originalText;
    private String standardTranslation;
    private List<TermHint> termHints;
    
    // 学生翻译
    private String studentTranslation;
    private String studentAudioPath;
    
    // 诊断报告
    private DiagnosisReport diagnosisReport;
    
    // 状态和时间戳
    private String status; // uploaded, processing, completed, graded, error
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private LocalDateTime gradedAt;

    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAudioFileName() { return audioFileName; }
    public void setAudioFileName(String audioFileName) { this.audioFileName = audioFileName; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public Long getAudioSize() { return audioSize; }
    public void setAudioSize(Long audioSize) { this.audioSize = audioSize; }

    public String getInterpretationDirection() { return interpretationDirection; }
    public void setInterpretationDirection(String interpretationDirection) { this.interpretationDirection = interpretationDirection; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getStandardTranslation() { return standardTranslation; }
    public void setStandardTranslation(String standardTranslation) { this.standardTranslation = standardTranslation; }

    public List<TermHint> getTermHints() { return termHints; }
    public void setTermHints(List<TermHint> termHints) { this.termHints = termHints; }

    public String getStudentTranslation() { return studentTranslation; }
    public void setStudentTranslation(String studentTranslation) { this.studentTranslation = studentTranslation; }

    public String getStudentAudioPath() { return studentAudioPath; }
    public void setStudentAudioPath(String studentAudioPath) { this.studentAudioPath = studentAudioPath; }

    public DiagnosisReport getDiagnosisReport() { return diagnosisReport; }
    public void setDiagnosisReport(DiagnosisReport diagnosisReport) { this.diagnosisReport = diagnosisReport; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
}
