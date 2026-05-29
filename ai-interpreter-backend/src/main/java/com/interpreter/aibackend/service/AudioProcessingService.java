package com.interpreter.aibackend.service;

import com.interpreter.aibackend.entity.InterpretationDirection;
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
import java.nio.file.Paths;
import java.util.List;

@Service
public class AudioProcessingService {
    private static final Logger logger = LoggerFactory.getLogger(AudioProcessingService.class);

    @Autowired
    private SessionService sessionService;

    @Autowired
    private BaiduAsrService baiduAsrService;

    @Autowired
    private BaiduLlmService baiduLlmService;

    @Autowired
    private AudioConversionService audioConversionService;

    @Value("${upload.dir}")
    private String uploadDir;

    public Session saveAudioFile(byte[] fileContent, String originalFileName) throws Exception {
        return saveAudioFile(fileContent, originalFileName, InterpretationDirection.EN_ZH.getCode());
    }

    public Session saveAudioFile(byte[] fileContent, String originalFileName, String directionCode) throws Exception {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String filePath = uploadDir + "/" + timestamp + "_" + originalFileName;
        Files.write(Paths.get(filePath), fileContent);

        String asrAudioPath = audioConversionService.convertToAsrWavIfNeeded(filePath);
        InterpretationDirection direction = InterpretationDirection.fromCode(directionCode);
        return sessionService.createSession(originalFileName, asrAudioPath, (long) fileContent.length, direction.getCode());
    }

    @Async
    public void processAudioAsync(String sessionId) {
        try {
            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                logger.error("Session not found: {}", sessionId);
                return;
            }

            InterpretationDirection direction = InterpretationDirection.fromCode(session.getInterpretationDirection());
            String originalText = baiduAsrService.transcribeAudio(session.getAudioPath(), direction.getSourceAsrDevPid());
            String standardTranslation = baiduLlmService.translateText(originalText, direction);
            List<TermHint> termHints = baiduLlmService.extractTerms(originalText, direction);

            sessionService.updateSessionWithTranslation(sessionId, originalText, standardTranslation, termHints);
            logger.info("Audio processing completed: {}", sessionId);
        } catch (Exception e) {
            logger.error("Audio processing failed: {}", sessionId, e);
            sessionService.setError(sessionId, e.getMessage());
        }
    }

    public void processStudentAudio(String sessionId, byte[] audioContent, String originalFileName) throws Exception {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists()) {
            uploadPath.mkdirs();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String extension = getFileExtension(originalFileName);
        if (extension.isEmpty()) {
            extension = "wav";
        }

        String studentAudioPath = uploadDir + "/" + timestamp + "_student." + extension;
        Files.write(Paths.get(studentAudioPath), audioContent);
        String asrStudentAudioPath = audioConversionService.convertToAsrWavIfNeeded(studentAudioPath);

        Session session = sessionService.getSession(sessionId);
        InterpretationDirection direction = InterpretationDirection.fromCode(session == null ? null : session.getInterpretationDirection());
        String studentTranslation = baiduAsrService.transcribeAudio(asrStudentAudioPath, direction.getTargetAsrDevPid());

        sessionService.updateStudentTranslation(sessionId, studentTranslation);
    }

    @Async
    public void processStudentAudioAndDiagnoseAsync(String sessionId, byte[] audioContent, String originalFileName) {
        try {
            processStudentAudio(sessionId, audioContent, originalFileName);

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                throw new RuntimeException("Session not found: " + sessionId);
            }
            if (session.getOriginalText() == null || session.getStudentTranslation() == null) {
                throw new RuntimeException("Missing original text or student translation");
            }

            InterpretationDirection direction = InterpretationDirection.fromCode(session.getInterpretationDirection());
            var report = baiduLlmService.diagnoseTranslation(
                    session.getOriginalText(),
                    session.getStandardTranslation(),
                    session.getStudentTranslation(),
                    direction
            );
            sessionService.updateDiagnosisReport(sessionId, report);
            logger.info("Student diagnosis completed: {}", sessionId);
        } catch (Exception e) {
            logger.error("Student audio diagnosis failed: {}", sessionId, e);
            sessionService.setError(sessionId, e.getMessage());
        }
    }

    @Async
    public void diagnoseStudentTranslation(String sessionId) {
        try {
            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                logger.error("Session not found: {}", sessionId);
                return;
            }
            if (session.getOriginalText() == null || session.getStudentTranslation() == null) {
                throw new RuntimeException("Missing original text or student translation");
            }

            InterpretationDirection direction = InterpretationDirection.fromCode(session.getInterpretationDirection());
            var report = baiduLlmService.diagnoseTranslation(
                    session.getOriginalText(),
                    session.getStandardTranslation(),
                    session.getStudentTranslation(),
                    direction
            );
            sessionService.updateDiagnosisReport(sessionId, report);
        } catch (Exception e) {
            logger.error("Diagnosis failed: {}", sessionId, e);
            sessionService.setError(sessionId, e.getMessage());
        }
    }

    public void cleanupOldAudioFiles(long maxAgeMillis) {
        File uploadPath = new File(uploadDir);
        if (!uploadPath.exists() || !uploadPath.isDirectory()) {
            return;
        }

        File[] files = uploadPath.listFiles();
        if (files == null) {
            return;
        }

        long now = System.currentTimeMillis();
        for (File file : files) {
            if (file.isFile() && (now - file.lastModified()) > maxAgeMillis) {
                file.delete();
            }
        }
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
}
