package com.interpreter.aibackend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import com.interpreter.aibackend.entity.DiagnosisReport;
import com.interpreter.aibackend.entity.InterpretationDirection;
import com.interpreter.aibackend.entity.TermHint;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BaiduLlmService {
    private static final Logger logger = LoggerFactory.getLogger(BaiduLlmService.class);

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(240, TimeUnit.SECONDS)
            .build();

    public String translateText(String sourceText) throws Exception {
        return translateText(sourceText, InterpretationDirection.EN_ZH);
    }

    public String translateText(String sourceText, InterpretationDirection direction) throws Exception {
        logger.info("Translating source text as {}", direction.getCode());

        String prompt = String.format("""
                You are a professional conference interpreter.
                Translate the following %s source text into polished %s.

                Source text:
                %s

                Requirements:
                - Preserve meaning, numbers, names, and technical terms.
                - Keep the style suitable for interpreting training.
                - Output only the translated text, with no explanation.
                """, direction.getSourceLanguage(), direction.getTargetLanguage(), sourceText);

        return callLlmApi(prompt);
    }

    public List<TermHint> extractTerms(String sourceText) throws Exception {
        return extractTerms(sourceText, InterpretationDirection.EN_ZH);
    }

    public List<TermHint> extractTerms(String sourceText, InterpretationDirection direction) throws Exception {
        logger.info("Extracting term hints as {}", direction.getCode());

        String prompt = String.format("""
                Extract 5 to 8 key terms, proper nouns, organizations, numbers, and difficult phrases from the following %s source text.
                Provide their %s equivalents for interpreting support.

                Source text:
                %s

                Output a JSON array only. Each item must contain:
                - term: the original %s term or phrase
                - translation: the %s equivalent
                - timestamp: estimated time in seconds, starting from 0
                - category: term, name, number, organization, or phrase
                """,
                direction.getSourceLanguage(),
                direction.getTargetLanguage(),
                sourceText,
                direction.getSourceLanguage(),
                direction.getTargetLanguage());

        String response = callLlmApi(prompt);
        return parseTermsFromJson(response);
    }

    public DiagnosisReport diagnoseTranslation(String originalText, String standardTranslation, String studentTranslation) throws Exception {
        return diagnoseTranslation(originalText, standardTranslation, studentTranslation, InterpretationDirection.EN_ZH);
    }

    public DiagnosisReport diagnoseTranslation(
            String originalText,
            String standardTranslation,
            String studentTranslation,
            InterpretationDirection direction
    ) throws Exception {
        logger.info("Diagnosing student interpretation as {}", direction.getCode());

        String prompt = String.format("""
                You are a professional interpreting teacher.
                Evaluate a student's %s to %s interpretation using the source text, reference translation, and student's transcript.

                Source text (%s):
                %s

                Reference translation (%s):
                %s

                Student interpretation transcript (%s):
                %s

                Analyze:
                1. accuracy: whether the core meaning is correctly conveyed.
                2. completeness: omissions or added content.
                3. fluency: naturalness and delivery quality reflected in the transcript.
                4. terminology: handling of terms, names, and numbers.

                Output JSON only with these fields:
                {
                  "score": 0-100,
                  "accuracy": "short paragraph",
                  "completeness": "short paragraph",
                  "fluency": "short paragraph",
                  "terminology": "short paragraph",
                  "main_issues": ["issue 1", "issue 2"],
                  "suggestions": ["suggestion 1", "suggestion 2"]
                }
                """,
                direction.getSourceLanguage(),
                direction.getTargetLanguage(),
                direction.getSourceLanguage(),
                originalText,
                direction.getTargetLanguage(),
                standardTranslation,
                direction.getTargetLanguage(),
                studentTranslation);

        String response = callLlmApi(prompt);
        return parseDiagnosisFromJson(response);
    }

    private String callLlmApi(String prompt) throws Exception {
        String apiKey = baiduApiConfig.getLlm().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            throw new RuntimeException("Baidu LLM API key is not configured");
        }

        JSONObject payload = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        payload.put("model", baiduApiConfig.getLlm().getModel());
        payload.put("messages", messages);
        payload.put("temperature", baiduApiConfig.getLlm().getTemperature());
        payload.put("top_p", baiduApiConfig.getLlm().getTopP());
        payload.put("max_tokens", baiduApiConfig.getLlm().getMaxOutputTokens());

        RequestBody body = RequestBody.create(
                payload.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url("https://qianfan.baidubce.com/v2/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        String responseBody;
        try (Response httpResponse = httpClient.newCall(request).execute()) {
            responseBody = httpResponse.body() == null ? "" : httpResponse.body().string();
            if (!httpResponse.isSuccessful()) {
                throw new RuntimeException("LLM HTTP error " + httpResponse.code() + ": " + responseBody);
            }
        }

        JSONObject response = JSON.parseObject(responseBody);
        if (response.containsKey("error")) {
            JSONObject error = response.getJSONObject("error");
            String errorMsg = error == null ? response.toJSONString() : error.getString("message");
            throw new RuntimeException("LLM error: " + errorMsg);
        }

        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("LLM returned empty choices");
        }

        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String result = message == null ? null : message.getString("content");
        if (result == null || result.isBlank()) {
            throw new RuntimeException("LLM returned empty content");
        }
        return result.trim();
    }

    private List<TermHint> parseTermsFromJson(String jsonStr) {
        List<TermHint> terms = new ArrayList<>();
        try {
            JSONArray jsonArray = JSON.parseArray(extractJsonArray(jsonStr));
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                terms.add(new TermHint(
                        item.getString("term"),
                        item.getString("translation"),
                        item.getDoubleValue("timestamp"),
                        item.getString("category")
                ));
            }
        } catch (Exception e) {
            logger.error("Failed to parse term JSON: {}", jsonStr, e);
        }
        return terms;
    }

    private DiagnosisReport parseDiagnosisFromJson(String jsonStr) {
        DiagnosisReport report = new DiagnosisReport();
        try {
            JSONObject json = JSON.parseObject(extractJsonObject(jsonStr));
            report.setScore(json.getIntValue("score"));
            report.setAccuracy(json.getString("accuracy"));
            report.setCompleteness(json.getString("completeness"));
            report.setFluency(json.getString("fluency"));
            report.setTerminology(json.getString("terminology"));

            JSONArray issuesArray = json.getJSONArray("main_issues");
            if (issuesArray == null) {
                issuesArray = json.getJSONArray("mainIssues");
            }
            if (issuesArray != null) {
                report.setMainIssues(issuesArray.toList(String.class));
            }

            JSONArray suggestionsArray = json.getJSONArray("suggestions");
            if (suggestionsArray != null) {
                report.setSuggestions(suggestionsArray.toList(String.class));
            }
        } catch (Exception e) {
            logger.error("Failed to parse diagnosis JSON: {}", jsonStr, e);
        }
        return report;
    }

    private String extractJsonArray(String value) {
        String cleaned = stripCodeFence(value);
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String extractJsonObject(String value) {
        String cleaned = stripCodeFence(value);
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String stripCodeFence(String value) {
        return value == null ? "" : value.replace("```json", "").replace("```", "").trim();
    }
}
