package com.interpreter.aibackend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度 API 基础服务（OAuth、HTTP 请求等）
 */
@Service
public class BaiduApiService {
    private static final Logger logger = LoggerFactory.getLogger(BaiduApiService.class);

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    private final OkHttpClient httpClient = new OkHttpClient();
    private String cachedAccessToken;
    private long tokenExpireTime;

    /**
     * 获取 Access Token (百度 OAuth)
     */
    public String getAccessToken() throws Exception {
        // 检查缓存的 token 是否还有效
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            logger.debug("使用缓存的 Access Token");
            return cachedAccessToken;
        }

        logger.info("获取新的 Access Token...");
        String url = "https://aip.baidubce.com/oauth/2.0/token";

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", baiduApiConfig.getLlm().getApiKey())
                .add("client_secret", baiduApiConfig.getLlm().getSecretKey())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed to get access token: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);

            if (jsonResponse.containsKey("error")) {
                throw new RuntimeException("OAuth error: " + jsonResponse.getString("error_description"));
            }

            cachedAccessToken = jsonResponse.getString("access_token");
            long expiresIn = jsonResponse.getLongValue("expires_in");

            // 设置过期时间 (提前 5 分钟过期)
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000;

            logger.info("✓ Access Token 获取成功，有效期：{} 秒", expiresIn);
            return cachedAccessToken;
        }
    }

    /**
     * 发送 HTTP POST 请求
     */
    public String postRequest(String url, String jsonBody) throws Exception {
        logger.debug("POST 请求: {}", url);

        RequestBody body = RequestBody.create(
                jsonBody,
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("HTTP 请求失败: " + response.code());
            }

            String responseBody = response.body().string();
            logger.debug("响应: {}", responseBody);
            return responseBody;
        }
    }

    /**
     * MD5 哈希计算（百度 ASR 需要）
     */
    public static String getMD5Hash(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        byte[] messageDigest = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : messageDigest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Base64 编码
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 解码
     */
    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data);
    }
}
