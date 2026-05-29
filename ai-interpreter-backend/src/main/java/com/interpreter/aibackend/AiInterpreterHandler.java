package com.interpreter.aibackend;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class AiInterpreterHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(AiInterpreterHandler.class);

    private final BaiduApiConfig baiduApiConfig;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public AiInterpreterHandler(BaiduApiConfig baiduApiConfig) {
        this.baiduApiConfig = baiduApiConfig;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JSONObject payload = JSON.parseObject(message.getPayload());
        String audioBase64 = payload.getString("audioData");

        String recognizedText = mockAsrProcess(audioBase64);

        if (recognizedText != null && !recognizedText.isEmpty()) {
            String hintsJsonStr = extractTermsWithBaiduLLM(recognizedText);

            JSONArray hintsArray = new JSONArray();
            // 【新增】安全保护：只有在拿到真实的非空结果时，才进行字符串处理
            if (hintsJsonStr != null && !hintsJsonStr.equals("[]") && !hintsJsonStr.isEmpty()) {
                try {
                    hintsJsonStr = hintsJsonStr.replaceAll("```json", "").replaceAll("```", "").trim();
                    hintsArray = JSON.parseArray(hintsJsonStr);
                } catch (Exception e) {
                    logger.warn("LLM hints JSON parse failed");
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("originalText", recognizedText);
            response.put("hints", hintsArray);

            session.sendMessage(new TextMessage(JSON.toJSONString(response)));
        }
    }

    private String mockAsrProcess(String audioBase64) {
        return "In 2023, the global GDP reached 105 trillion dollars, and artificial intelligence became a major catalyst.";
    }

    private String extractTermsWithBaiduLLM(String text) throws IOException {
        String apiKey = baiduApiConfig.getLlm().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            logger.warn("Baidu LLM API key is not configured for WebSocket hints");
            return "[]";
        }

        // 1. 升级为全新的 V2 接口地址（OpenAI 兼容标准）
        String url = "https://qianfan.baidubce.com/v2/chat/completions";

        String prompt = "你是一个口译辅助AI。请提取以下英文句子中的数字(number)和专有名词(term)，并提供准确的中文翻译。" +
                "必须仅返回一个JSON数组，格式如下：[{\"type\":\"number\"或\"term\", \"source\":\"英文原词\", \"translation\":\"中文翻译\"}]。" +
                "不要包含任何说明文字。句子：" + text;

        JSONObject messageObj = new JSONObject();
        messageObj.put("role", "user");
        messageObj.put("content", prompt);

        JSONObject requestBody = new JSONObject();
        // 2. 在 JSON 请求体中动态指定你拥有 100万免费额度的新模型！
        // 如果想换成 DeepSeek，直接把这里改成 "deepseek-v3" 即可
        requestBody.put("model", baiduApiConfig.getLlm().getModel()); 
        requestBody.put("messages", new JSONObject[]{messageObj});
        requestBody.put("temperature", 0.1);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.body() != null) {
                String responseStr = response.body().string();
                if (response.isSuccessful()) {
                    JSONObject resultJson = JSON.parseObject(responseStr);
                    // 3. 解析 OpenAI 标准格式的返回体：一层层剥开 choices -> message -> content
                    return resultJson.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                } else {
                    logger.warn("Baidu LLM hints request failed, status={}", response.code());
                }
            }
        } catch (Exception e) {
            logger.warn("Baidu LLM hints request failed: {}", e.getMessage());
        }
        return "[]";
    }
}
