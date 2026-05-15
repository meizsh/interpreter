# 🔌 百度 API 集成配置指南

## 目录
1. [百度 ASR 语音识别](#1-百度-asr-语音识别)
2. [百度千帆大模型](#2-百度千帆大模型ernie-x1-turbo)
3. [环境变量配置](#3-环境变量配置)
4. [Java 集成代码示例](#4-java-集成代码示例)
5. [测试脚本](#5-测试脚本)

---

## 1. 百度 ASR 语音识别

### API 信息
- **服务名**: 百度语音识别
- **官网**: https://ai.baidu.com/ai-doc/SPEECH/Tl39k86hp
- **应用场景**: 
  - 将音频原文转写为文本
  - 将学生口译录音转写为文本

### 获取凭证步骤

1. **登录百度智能云**: https://login.bce.baidu.com
2. **创建应用**:
   - 进入"应用列表" → "创建应用"
   - 选择"语音识别"服务
   - 填写应用名称、选择"个人"
   - 点击"创建应用"
3. **获取密钥**:
   - 点击应用名称进入详情页
   - 记录 **API Key** 和 **Secret Key**

### 配置示例

```properties
# application.properties
baidu.asr.api-key=your-asr-api-key-here
baidu.asr.secret-key=your-asr-secret-key-here
baidu.asr.app-id=your-app-id-here

# ASR 特定配置
baidu.asr.format=mp3      # 支持: pcm, wav, mp3, m4a
baidu.asr.rate=16000      # 采样率
baidu.asr.language=zh-CN  # 语言: zh-CN, en-US, etc
```

### 支持的音频格式

| 格式 | 采样率 | 位深 | 通道 |
|------|--------|------|------|
| WAV | 16000 Hz | 16 bit | Mono |
| MP3 | 16000 Hz | - | Mono |
| M4A | 16000 Hz | - | Mono |
| PCM | 16000 Hz | 16 bit | Mono |

### API 调用示例 (HTTP POST)

```bash
# 获取 Access Token
POST https://aip.baidubce.com/oauth/2.0/token
Parameters:
  - grant_type: client_credentials
  - client_id: {API_KEY}
  - client_secret: {SECRET_KEY}

Response:
{
  "access_token": "24.abc123...",
  "expires_in": 2592000,
  "session_key": "...",
  "refresh_token": "...",
  "scope": "brain_speech_recognition"
}

# 调用 ASR 接口
POST https://vop.baidu.com/server_api
Headers:
  - Content-Type: application/x-www-form-urlencoded
  
Parameters (form-data):
  - speech: <base64编码的音频数据>
  - format: mp3
  - rate: 16000
  - cuid: <唯一用户ID>
  - token: {access_token}

Response:
{
  "err_no": 0,
  "err_msg": "success.",
  "result": ["识别结果文本"]
}
```

---

## 2. 百度千帆大模型 (ERNIE-X1-Turbo)

### API 信息
- **服务名**: 百度千帆大模型
- **官网**: https://cloud.baidu.com/doc/WENXINWORKSHOP/s/clntwmv76
- **模型**: ERNIE-X1-Turbo-32K (32K token 上下文)
- **应用场景**:
  - 生成高质量中文翻译
  - 提取术语和关键概念
  - 进行口译诊断和评分

### 获取凭证步骤

1. **登录百度云控制台**: https://console.bce.baidu.com
2. **创建应用**:
   - 左侧菜单 → "应用列表" → "创建应用"
   - 应用服务选择"百度千帆"
   - 填写应用名称
   - 点击"创建"
3. **获取密钥**:
   - 点击应用名称进入详情
   - 记录 **API Key** 和 **Secret Key**

### 配置示例

```properties
# application.properties
baidu.llm.api-key=your-llm-api-key-here
baidu.llm.secret-key=your-llm-secret-key-here
baidu.llm.model=ernie-3.5-8k-0701    # 或 ernie-x1-turbo-32k

# 模型参数
baidu.llm.temperature=0.7            # 创意度 (0-1)
baidu.llm.top-p=0.9                  # 多样性 (0-1)
baidu.llm.max-output-tokens=2000     # 最大输出长度
```

### API 调用示例

```bash
# 获取 Access Token
POST https://aip.baidubce.com/oauth/2.0/token
Parameters:
  - grant_type: client_credentials
  - client_id: {API_KEY}
  - client_secret: {SECRET_KEY}

# 调用文本生成接口
POST https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-3.5-8k-0701
Header:
  - Authorization: Bearer {access_token}
  - Content-Type: application/json

Request Body:
{
  "messages": [
    {
      "role": "user",
      "content": "将这段英文翻译成中文：[原文内容]"
    }
  ],
  "temperature": 0.7,
  "top_p": 0.9,
  "max_output_tokens": 2000
}

Response:
{
  "id": "as-...",
  "object": "chat.completion",
  "created": 1234567890,
  "result": "翻译结果...",
  "is_truncated": false,
  "need_clear_history": false,
  "usage": {
    "prompt_tokens": 123,
    "completion_tokens": 456,
    "total_tokens": 579
  }
}
```

### 常用 Prompt 模板

#### 1. 翻译 Prompt
```
你是一名专业口译员。请将下列英文内容翻译成标准的中文，保留原意和风格。

原文：
{source_text}

要求：
- 翻译必须准确、流畅
- 保留专业术语的原文形式（如常见英文术语）
- 输出格式为纯中文文本，不需要额外说明
```

#### 2. 术语提取 Prompt
```
从下列文本中提取关键术语、专有名词和重要概念，并给出中文翻译。

文本：
{source_text}

请输出格式为：
术语 | 中文翻译 | 出现位置(单词数)
------|----------|----------------
Digital Economy | 数字经济 | 0-2
Sustainable Development | 可持续发展 | 10-12

只输出表格内容，不需要其他说明。
```

#### 3. 口译诊断 Prompt
```
作为专业口译教师，请对以下三方文本进行诊断分析。

【源文】
{original_text}

【标准参考答案】
{standard_translation}

【学生的口译转写】
{student_translation}

请从以下维度进行分析：
1. 准确度：是否正确传达了源文的核心意思
2. 完整性：是否有遗漏关键信息（漏译）
3. 流畅性：表达是否自然流畅
4. 术语准确性：专业术语是否恰当

最后给出：
- 综合评分 (0-100)
- 主要问题 (列表)
- 改进建议 (列表)

输出格式为 JSON，包含以下字段：
{
  "score": 88,
  "accuracy": "准确度分析...",
  "completeness": "完整性分析...",
  "fluency": "流畅性分析...",
  "terminology": "术语准确性分析...",
  "main_issues": ["issue1", "issue2"],
  "suggestions": ["suggestion1", "suggestion2"]
}
```

---

## 3. 环境变量配置

### Spring Boot 配置文件 (application.properties)

```properties
# ========== 服务器配置 ==========
server.port=8080
server.servlet.context-path=/
spring.application.name=ai-interpreter-backend

# ========== 百度 ASR 配置 ==========
baidu.asr.api-key=${BAIDU_ASR_API_KEY}
baidu.asr.secret-key=${BAIDU_ASR_SECRET_KEY}
baidu.asr.app-id=${BAIDU_ASR_APP_ID}
baidu.asr.format=mp3
baidu.asr.rate=16000
baidu.asr.language=zh-CN

# ========== 百度千帆 LLM 配置 ==========
baidu.llm.api-key=${BAIDU_LLM_API_KEY}
baidu.llm.secret-key=${BAIDU_LLM_SECRET_KEY}
baidu.llm.model=ernie-3.5-8k-0701
baidu.llm.temperature=0.7
baidu.llm.top-p=0.9
baidu.llm.max-output-tokens=2000

# ========== 前端配置 ==========
frontend.base-url=http://localhost:5173
cors.allowed-origins=http://localhost:5173,http://localhost:3000

# ========== 文件存储配置 ==========
upload.dir=./uploads
upload.max-size=104857600  # 100MB
upload.allowed-formats=mp3,wav,m4a

# ========== 日志配置 ==========
logging.level.root=INFO
logging.level.com.interpreter=DEBUG
logging.level.org.springframework.web=INFO

# ========== 缓存配置 ==========
spring.cache.type=simple
spring.session.store-type=memory
```

### 环境变量设置 (Windows PowerShell)

```powershell
# 设置环境变量
$env:BAIDU_ASR_API_KEY = "your-asr-api-key"
$env:BAIDU_ASR_SECRET_KEY = "your-asr-secret-key"
$env:BAIDU_ASR_APP_ID = "your-asr-app-id"

$env:BAIDU_LLM_API_KEY = "your-llm-api-key"
$env:BAIDU_LLM_SECRET_KEY = "your-llm-secret-key"

# 验证
Get-ChildItem env:BAIDU_*
```

### 前端环境变量 (.env)

```env
# .env (前端根目录)
VITE_API_URL=http://localhost:8080
VITE_WS_URL=ws://localhost:8080/ws
VITE_API_TIMEOUT=30000

# 调试模式
VITE_DEBUG=true
```

---

## 4. Java 集成代码示例

### BaiduAsrService.java

```java
package com.interpreter.aibackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.baidu.speech.api.client.SpeechClient;
import com.baidu.speech.api.model.SpeechRequest;
import com.baidu.speech.api.model.SpeechResponse;

@Service
public class BaiduAsrService {

    @Value("${baidu.asr.api-key}")
    private String apiKey;

    @Value("${baidu.asr.secret-key}")
    private String secretKey;

    @Value("${baidu.asr.format}")
    private String format;

    /**
     * 将音频文件转写为文本
     */
    public String transcribeAudio(byte[] audioData) throws Exception {
        // Step 1: 获取 Access Token
        String accessToken = getAccessToken();

        // Step 2: 构建请求
        SpeechRequest request = new SpeechRequest()
            .setFormat(format)
            .setRate(16000)
            .setLanguage("zh-CN")
            .setAudioData(audioData);

        // Step 3: 调用 API
        SpeechResponse response = SpeechClient.transcribe(accessToken, request);

        // Step 4: 返回结果
        if (response.getErrNo() == 0) {
            return response.getResult().get(0);
        } else {
            throw new RuntimeException("ASR Error: " + response.getErrMsg());
        }
    }

    /**
     * 获取 Access Token
     */
    private String getAccessToken() throws Exception {
        // 实现百度 OAuth 认证获取 token
        // 详见: https://ai.baidu.com/ai-doc/REFERENCE/Ck3dwjhhu
        String url = "https://aip.baidubce.com/oauth/2.0/token";
        // 使用 apiKey 和 secretKey 获取 token...
        return ""; // 返回 access_token
    }
}
```

### BaiduLLMService.java

```java
package com.interpreter.aibackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class BaiduLLMService {

    @Value("${baidu.llm.api-key}")
    private String apiKey;

    @Value("${baidu.llm.secret-key}")
    private String secretKey;

    @Value("${baidu.llm.model}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 翻译原文
     */
    public String translateText(String sourceText) throws Exception {
        String prompt = String.format(
            "你是一名专业口译员。请将下列英文翻译成中文：\n\n%s\n\n只输出翻译结果，不需要其他说明。",
            sourceText
        );
        return callLLM(prompt);
    }

    /**
     * 提取术语和时间轴
     */
    public List<TermHint> extractTerms(String sourceText, String audioUrl) throws Exception {
        String prompt = String.format(
            "从下列文本中提取5-8个关键术语，并估算每个术语在音频中出现的大致时间位置。\n\n"
            + "文本：\n%s\n\n"
            + "输出格式为 JSON 数组，每个元素包含：\n"
            + "{\"term\": \"英文术语\", \"translation\": \"中文翻译\", \"timestamp\": 2.5}\n\n"
            + "只输出 JSON 数组，不需要其他说明。",
            sourceText
        );

        String response = callLLM(prompt);
        // 解析 JSON 并转换为 TermHint 对象
        return parseTermsFromJson(response);
    }

    /**
     * 诊断学生口译
     */
    public DiagnosisReport diagnoseStudentTranslation(
            String originalText,
            String standardTranslation,
            String studentTranslation) throws Exception {

        String prompt = String.format(
            "作为口译教师，诊断以下三方文本。\n\n"
            + "【源文】%s\n\n"
            + "【标准答案】%s\n\n"
            + "【学生翻译】%s\n\n"
            + "输出 JSON 包含：score (0-100), accuracy, completeness, fluency, "
            + "main_issues (数组), suggestions (数组)",
            originalText, standardTranslation, studentTranslation
        );

        String response = callLLM(prompt);
        return parseReportFromJson(response);
    }

    /**
     * 调用大模型 API
     */
    private String callLLM(String prompt) throws Exception {
        String accessToken = getAccessToken();

        // 构建请求 JSON
        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", List.of(
            Map.of("role", "user", "content", prompt)
        ));
        payload.put("temperature", 0.7);
        payload.put("max_output_tokens", 2000);

        // 调用 API
        String url = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/" + model
                   + "?access_token=" + accessToken;

        // 使用 HttpClient 或 RestTemplate 发送请求
        // 获取响应并返回 result 字段
        return ""; // 返回 LLM 的输出
    }

    /**
     * 获取 Access Token
     */
    private String getAccessToken() throws Exception {
        // 实现百度 OAuth 认证获取 token
        return ""; // 返回 access_token
    }

    // 辅助方法...
    private List<TermHint> parseTermsFromJson(String jsonStr) { /* ... */ }
    private DiagnosisReport parseReportFromJson(String jsonStr) { /* ... */ }
}
```

### 完整的 OAuth 认证实现

```java
/**
 * 百度 OAuth 认证工具类
 */
@Service
public class BaiduOAuthService {

    private static final String TOKEN_URL = "https://aip.baidubce.com/oauth/2.0/token";
    private final RestTemplate restTemplate = new RestTemplate();
    private String cachedToken;
    private long tokenExpireTime;

    @Value("${baidu.asr.api-key}")
    private String asrApiKey;

    @Value("${baidu.asr.secret-key}")
    private String asrSecretKey;

    /**
     * 获取 Access Token (带缓存)
     */
    public String getAccessToken() throws Exception {
        // 检查缓存的 token 是否还有效
        if (cachedToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedToken;
        }

        // 请求新 token
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "client_credentials");
        params.put("client_id", asrApiKey);
        params.put("client_secret", asrSecretKey);

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                TOKEN_URL + "?grant_type={grant_type}&client_id={client_id}&client_secret={client_secret}",
                Map.class,
                params
            );

            Map<String, Object> body = response.getBody();
            cachedToken = (String) body.get("access_token");
            Long expiresIn = ((Number) body.get("expires_in")).longValue();

            // 设置过期时间 (提前 5 分钟过期)
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 300) * 1000;

            return cachedToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get access token", e);
        }
    }
}
```

---

## 5. 测试脚本

### 测试 ASR 功能

```bash
#!/bin/bash
# test_asr.sh

API_URL="http://localhost:8080/api/audio/upload"
AUDIO_FILE="./test_audio.mp3"

echo "📤 上传音频文件..."
response=$(curl -X POST \
  -F "file=@$AUDIO_FILE" \
  "$API_URL")

echo "📋 响应："
echo "$response" | jq .

# 提取 session_id
session_id=$(echo "$response" | jq -r '.session_id')

echo "⏳ 等待 3 秒处理完成..."
sleep 3

echo "📥 查询元数据..."
curl "$API_URL/../session/$session_id/metadata" | jq .
```

### 测试 LLM 功能

```bash
#!/bin/bash
# test_llm.sh

API_URL="http://localhost:8080/api"

# 测试翻译
curl -X POST "$API_URL/translate" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "The digital economy is driving sustainable development"
  }' | jq .

# 测试术语提取
curl -X POST "$API_URL/extract-terms" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "The digital economy is driving sustainable development"
  }' | jq .

# 测试诊断
curl -X POST "$API_URL/diagnose" \
  -H "Content-Type: application/json" \
  -d '{
    "original": "The digital economy is driving sustainable development",
    "standard": "数字经济正在推动可持续发展",
    "student": "数字经济推动发展"
  }' | jq .
```

---

## 🚀 快速开始

### 1. 获取百度 API Key

访问:
- ASR: https://ai.baidu.com/ai-doc/SPEECH/Tl39k86hp
- LLM: https://cloud.baidu.com/doc/WENXINWORKSHOP/s/clntwmv76

### 2. 设置环境变量

```powershell
$env:BAIDU_ASR_API_KEY = "..."
$env:BAIDU_ASR_SECRET_KEY = "..."
$env:BAIDU_LLM_API_KEY = "..."
$env:BAIDU_LLM_SECRET_KEY = "..."
```

### 3. 启动应用

```bash
cd ai-interpreter-backend
mvn spring-boot:run
```

### 4. 测试接口

```bash
bash test_asr.sh
bash test_llm.sh
```

---

**注意**: 所有 API Key 应存储在安全的环境变量或密钥管理系统中，不要提交到代码仓库。

