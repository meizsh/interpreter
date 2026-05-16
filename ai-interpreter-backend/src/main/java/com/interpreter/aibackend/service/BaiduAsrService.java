package com.interpreter.aibackend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度语音识别 (ASR) 服务
 */
@Service
public class BaiduAsrService {
    private static final Logger logger = LoggerFactory.getLogger(BaiduAsrService.class);

    @Autowired
    private BaiduApiService baiduApiService;

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * 识别音频文件并返回文本
     */
    public String transcribeAudio(String audioFilePath) throws Exception {
        logger.info("🎤 开始语音识别: {}", audioFilePath);

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists()) {
            throw new IOException("音频文件不存在: " + audioFilePath);
        }

        // 读取音频文件
        byte[] audioData = readFileToByteArray(audioFile);
        logger.info("✓ 音频文件大小: {} 字节", audioData.length);

        // 获取 access token
        String accessToken = baiduApiService.getAccessToken(true);

        // 调用百度 ASR API
        String format = getAsrFormat(audioFile.getName());
        int rate = getAsrRate(audioFile);
        String result = callBaiduAsrApi(audioData, accessToken, format, rate);

        logger.info("✓ 语音识别完成");
        return result;
    }

    /**
     * 调用百度 ASR API
     */
    private String callBaiduAsrApi(byte[] audioData, String accessToken, String format, int rate) throws Exception {
        String url = "https://vop.baidu.com/server_api";

        // 计算 MD5 和 speech 参数
        String md5 = BaiduApiService.getMD5Hash(audioData);
        String speech = BaiduApiService.encodeBase64(audioData);
        long cuid = System.currentTimeMillis(); // 唯一用户ID

        // 构建表单数据
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("speech", speech)
                .addFormDataPart("format", format)
                .addFormDataPart("rate", String.valueOf(rate))
                .addFormDataPart("cuid", String.valueOf(cuid))
                .addFormDataPart("token", accessToken)
                .addFormDataPart("len", String.valueOf(audioData.length))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("❌ ASR API 返回错误: {}", response.code());
                throw new RuntimeException("ASR API 失败: " + response.code());
            }

            String responseBody = response.body().string();
            logger.debug("ASR 响应: {}", responseBody);

            JSONObject jsonResponse = JSON.parseObject(responseBody);

            // 检查错误
            if (jsonResponse.getIntValue("err_no") != 0) {
                String errMsg = jsonResponse.getString("err_msg");
                logger.error("❌ ASR 错误: {}", errMsg);
                throw new RuntimeException("ASR 错误: " + errMsg);
            }

            // 提取识别结果
            String result = jsonResponse.getJSONArray("result").getString(0);
            logger.info("✓ 识别结果: {}", result);
            return result;
        }
    }

    private String getAsrFormat(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".wav")) {
            return "wav";
        }
        if (lower.endsWith(".mp3")) {
            return "mp3";
        }
        if (lower.endsWith(".m4a")) {
            return "wav";
        }
        return baiduApiConfig.getAsr().getFormat();
    }

    private int getAsrRate(File file) {
        String filename = file.getName().toLowerCase();
        if (filename.endsWith(".wav")) {
            try {
                return readWavSampleRate(file);
            } catch (Exception e) {
                logger.warn("无法读取 WAV 采样率，使用默认值 {}", baiduApiConfig.getAsr().getRate(), e);
                return baiduApiConfig.getAsr().getRate();
            }
        }
        return baiduApiConfig.getAsr().getRate();
    }

    private int readWavSampleRate(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[44];
            int read = fis.read(header);
            if (read < 44) {
                throw new IOException("WAV 文件头太短");
            }
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(header, 24, 4);
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            return buffer.getInt();
        }
    }

    /**
     * 读取文件到字节数组
     */
    private byte[] readFileToByteArray(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(data);
        }
        return data;
    }
}
