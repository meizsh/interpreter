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
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AudioController {
    private static final Logger logger = LoggerFactory.getLogger(AudioController.class);

    @Autowired
    private AudioProcessingService audioProcessingService;

    @Autowired
    private SessionService sessionService;

    /**
     * 1. 濠曟棁顔夐崢鐔肩叾娑撳﹣绱堕敍鍫Ｐ曢崣?AI 閼奉亜濮╁ú妤€鍤敍姘斧閺?+ 閺嶅洨鐡?+ 閺堫垵顕㈡潪杈剧礆
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "direction", defaultValue = "en-zh") String direction) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", -1, "message", "閺傚洣娆㈡稉铏光敄"));
            }

            String filename = file.getOriginalFilename();
            // 娣囨繂鐡ㄩ獮璺哄灡瀵よ桨绱扮拠?
            Session session = audioProcessingService.saveAudioFile(file.getBytes(), filename, direction);
            logger.info("閴?閸樼喖鐓跺韫瑐娴肩媴绱濆┑鈧ú?Session ID: {}", session.getSessionId());

            // 瀵倹顒炵憴锕€褰傞崥搴″酱婢跺嫮鎮婇柧鎹愮熅 (ASR + 缂堟槒鐦?+ 閺堫垵顕㈤幓鎰絿)
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
     * 2. 閹恒儲鏁圭€涳妇鏁撻崣锝堢槯瑜版洟鐓堕獮韬测偓鎰倱濮濄儳鐡戝鍛扮槑閸楀嘲鐣幋鎰┾偓鎴礄閻礁骞撻崜宥囶伂婢跺秵娼呴惃鍕樋濞喡ょ枂鐠囶澁绱?
     */
    @PostMapping("/{sessionId}/student-audio")
    public ResponseEntity<?> uploadStudentAudio(
            @PathVariable String sessionId,
            @RequestParam("studentAudio") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("code", -1, "message", "Student audio file is empty"));
            }

            Session session = sessionService.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("code", -1, "message", "Session not found"));
            }

            audioProcessingService.processStudentAudioAndDiagnoseAsync(
                    sessionId,
                    file.getBytes(),
                    file.getOriginalFilename()
            );

            JSONObject response = new JSONObject();
            response.put("code", 0);
            response.put("status", "processing");
            response.put("message", "Student audio accepted for diagnosis");
            response.put("session_id", sessionId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            logger.error("Student audio upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", -1, "message", e.getMessage()));
        }
    }
}
