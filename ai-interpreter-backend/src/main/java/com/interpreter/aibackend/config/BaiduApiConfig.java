package com.interpreter.aibackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 百度 API 配置
 */
@Component
@ConfigurationProperties(prefix = "baidu")
public class BaiduApiConfig {

    // ASR (语音识别) 配置
    private Asr asr = new Asr();
    
    // LLM (大模型) 配置
    private Llm llm = new Llm();

    public static class Asr {
        private String apiKey;
        private String secretKey;
        private String appId;
        private String format = "mp3";
        private int rate = 16000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getAppId() { return appId; }
        public void setAppId(String appId) { this.appId = appId; }

        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }

        public int getRate() { return rate; }
        public void setRate(int rate) { this.rate = rate; }
    }

    public static class Llm {
        private String apiKey;
        private String secretKey;
        private String model = "ernie-3.5-8k-0701";
        private double temperature = 0.7;
        private double topP = 0.9;
        private int maxOutputTokens = 2000;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public double getTemperature() { return temperature; }
        public void setTemperature(double temperature) { this.temperature = temperature; }

        public double getTopP() { return topP; }
        public void setTopP(double topP) { this.topP = topP; }

        public int getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    }

    public Asr getAsr() { return asr; }
    public void setAsr(Asr asr) { this.asr = asr; }

    public Llm getLlm() { return llm; }
    public void setLlm(Llm llm) { this.llm = llm; }
}
