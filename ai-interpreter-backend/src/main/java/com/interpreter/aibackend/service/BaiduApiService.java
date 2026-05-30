package com.interpreter.aibackend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.Base64;

/**
 * 百度 API 基础服务。
 * 负责 OAuth Access Token 获取、HTTP 请求，以及 ASR 所需的通用编码工具。
 */
@Service
public class BaiduApiService {
    private static final Logger logger = LoggerFactory.getLogger(BaiduApiService.class);

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    private final OkHttpClient httpClient = new OkHttpClient();
    private String cachedAsrAccessToken;
    private long asrTokenExpireTime;
    private String cachedLlmAccessToken;
    private long llmTokenExpireTime;

    /**
     * 获取百度 OAuth Access Token。
     * ASR 仍使用 OAuth 方式；新版千帆 LLM 使用 Bearer API Key，不依赖这个方法。
     */
    public String getAccessToken(boolean useAsrCredentials) throws Exception {
        String cachedAccessToken = useAsrCredentials ? cachedAsrAccessToken : cachedLlmAccessToken;
        long tokenExpireTime = useAsrCredentials ? asrTokenExpireTime : llmTokenExpireTime;
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            logger.debug("使用缓存的 Access Token");
            return cachedAccessToken;
        }

        logger.info("正在获取新的百度 Access Token");
        String apiKey = useAsrCredentials ? baiduApiConfig.getAsr().getApiKey() : baiduApiConfig.getLlm().getApiKey();
        String secretKey = useAsrCredentials ? baiduApiConfig.getAsr().getSecretKey() : baiduApiConfig.getLlm().getSecretKey();

        RequestBody body = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", apiKey)
                .add("client_secret", secretKey)
                .build();

        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("获取百度 Access Token 失败，HTTP 状态码：" + response.code());
            }

            String responseBody = response.body() == null ? "" : response.body().string();
            JSONObject jsonResponse = JSON.parseObject(responseBody);
            if (jsonResponse.containsKey("error")) {
                throw new RuntimeException("百度 OAuth 错误：" + jsonResponse.getString("error_description"));
            }

            String accessToken = jsonResponse.getString("access_token");
            long expiresIn = jsonResponse.getLongValue("expires_in");

            // 提前 5 分钟过期，避免临界时间请求失败。
            long expiresAt = System.currentTimeMillis() + (expiresIn - 300) * 1000;
            if (useAsrCredentials) {
                cachedAsrAccessToken = accessToken;
                asrTokenExpireTime = expiresAt;
            } else {
                cachedLlmAccessToken = accessToken;
                llmTokenExpireTime = expiresAt;
            }

            logger.info("百度 Access Token 获取成功，有效期 {} 秒", expiresIn);
            return accessToken;
        }
    }

    /**
     * 发送 JSON POST 请求。
     */
    public String postRequest(String url, String jsonBody) throws Exception {
        logger.debug("发送 POST 请求：{}", url);

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
                throw new RuntimeException("HTTP 请求失败，状态码：" + response.code());
            }

            String responseBody = response.body() == null ? "" : response.body().string();
            logger.debug("HTTP 响应：{}", responseBody);
            return responseBody;
        }
    }

    /**
     * 计算 MD5 哈希。
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
     * Base64 编码。
     */
    public static String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 解码。
     */
    public static byte[] decodeBase64(String data) {
        return Base64.getDecoder().decode(data);
    }
}
