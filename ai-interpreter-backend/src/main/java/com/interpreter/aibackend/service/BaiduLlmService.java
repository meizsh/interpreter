package com.interpreter.aibackend.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.interpreter.aibackend.config.BaiduApiConfig;
import com.interpreter.aibackend.entity.DiagnosisReport;
import com.interpreter.aibackend.entity.TermHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 百度千帆大模型 (LLM) 服务
 * 用于翻译、术语提取、诊断等任务
 */
@Service
public class BaiduLlmService {
    private static final Logger logger = LoggerFactory.getLogger(BaiduLlmService.class);

    @Autowired
    private BaiduApiService baiduApiService;

    @Autowired
    private BaiduApiConfig baiduApiConfig;

    /**
     * 翻译原文为中文
     */
    public String translateText(String sourceText) throws Exception {
        logger.info("🔄 开始翻译原文...");

        String prompt = String.format(
                "你是一名专业口译员。请将下列英文翻译成标准的中文，保留原意和风格。\n\n" +
                "原文：\n%s\n\n" +
                "要求：\n" +
                "- 翻译必须准确、流畅\n" +
                "- 保留专业术语的原文形式\n" +
                "- 只输出中文翻译文本，不需要额外说明",
                sourceText
        );

        String result = callLlmApi(prompt);
        logger.info("✓ 翻译完成");
        return result;
    }

    /**
     * 从原文中提取术语和时间轴
     */
    public List<TermHint> extractTerms(String sourceText) throws Exception {
        logger.info("📋 开始提取术语...");

        String prompt = String.format(
                "从下列文本中提取5-8个关键术语、专有名词和长数字，并估算每个术语在音频中大致出现的时间位置。\n\n" +
                "文本：\n%s\n\n" +
                "输出格式为 JSON 数组，每个元素包含以下字段：\n" +
                "- term: 英文术语\n" +
                "- translation: 中文翻译\n" +
                "- timestamp: 预计出现时间（秒，从0开始）\n" +
                "- category: 分类（术语、人名、数字等）\n\n" +
                "示例：\n" +
                "[\n" +
                "  {\"term\": \"Digital Economy\", \"translation\": \"数字经济\", \"timestamp\": 2.5, \"category\": \"术语\"},\n" +
                "  {\"term\": \"Sustainable Development\", \"translation\": \"可持续发展\", \"timestamp\": 5.0, \"category\": \"术语\"}\n" +
                "]\n\n" +
                "只输出 JSON 数组，不需要其他说明。",
                sourceText
        );

        String response = callLlmApi(prompt);
        return parseTermsFromJson(response);
    }

    /**
     * 诊断学生的口译表达
     */
    public DiagnosisReport diagnoseTranslation(String originalText, String standardTranslation, String studentTranslation) throws Exception {
        logger.info("📊 开始诊断学生口译...");

        String prompt = String.format(
                "你是一名专业口译教师。请对以下三方文本进行诊断分析。\n\n" +
                "【源文】\n%s\n\n" +
                "【标准参考答案】\n%s\n\n" +
                "【学生的口译转写】\n%s\n\n" +
                "请从以下维度进行分析：\n" +
                "1. 准确度：是否正确传达了源文的核心意思\n" +
                "2. 完整性：是否有遗漏关键信息（漏译）\n" +
                "3. 流畅性：表达是否自然流畅\n" +
                "4. 术语准确性：专业术语是否恰当\n\n" +
                "输出格式为 JSON，包含以下字段：\n" +
                "- score: 综合评分 (0-100)\n" +
                "- accuracy: 准确度分析 (一段文字)\n" +
                "- completeness: 完整性分析 (一段文字)\n" +
                "- fluency: 流畅性分析 (一段文字)\n" +
                "- terminology: 术语准确性分析 (一段文字)\n" +
                "- main_issues: 主要问题列表 (数组)\n" +
                "- suggestions: 改进建议列表 (数组)\n\n" +
                "示例：\n" +
                "{\n" +
                "  \"score\": 88,\n" +
                "  \"accuracy\": \"学生准确传达了源文的核心内容...\",\n" +
                "  \"completeness\": \"存在一处漏译：'sustainable'（可持续）...\",\n" +
                "  \"fluency\": \"表达基本流畅，但个别词汇选择可更地道...\",\n" +
                "  \"terminology\": \"术语运用准确，如'数字经济'、'中小企业'等...\",\n" +
                "  \"main_issues\": [\"漏译关键修饰词\", \"个别词汇搭配不当\"],\n" +
                "  \"suggestions\": [\"加强对修饰词的敏感度\", \"积累更多地道表达\"]\n" +
                "}\n\n" +
                "只输出 JSON，不需要其他说明。",
                originalText, standardTranslation, studentTranslation
        );

        String response = callLlmApi(prompt);
        return parseDiagnosisFromJson(response);
    }

    /**
     * 调用百度 LLM API (V2 标准)
     */
    private String callLlmApi(String prompt) throws Exception {
        String accessToken = baiduApiService.getAccessToken();
        String url = String.format(
                "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/%s?access_token=%s",
                baiduApiConfig.getLlm().getModel(),
                accessToken
        );

        // 构建请求体
        JSONObject payload = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        payload.put("messages", messages);
        payload.put("temperature", baiduApiConfig.getLlm().getTemperature());
        payload.put("top_p", baiduApiConfig.getLlm().getTopP());
        payload.put("max_output_tokens", baiduApiConfig.getLlm().getMaxOutputTokens());

        String jsonBody = payload.toJSONString();
        logger.debug("LLM 请求: {}", jsonBody);

        // 发送请求
        String responseBody = baiduApiService.postRequest(url, jsonBody);
        logger.debug("LLM 响应: {}", responseBody);

        // 解析响应
        JSONObject response = JSON.parseObject(responseBody);

        // 检查错误
        if (response.containsKey("error_code")) {
            String errorMsg = response.getString("error_msg");
            logger.error("❌ LLM 错误: {}", errorMsg);
            throw new RuntimeException("LLM 错误: " + errorMsg);
        }

        String result = response.getString("result");
        if (result == null) {
            logger.error("❌ LLM 返回空结果");
            throw new RuntimeException("LLM 返回空结果");
        }

        logger.info("✓ LLM 调用成功");
        return result;
    }

    /**
     * 从 JSON 字符串解析术语列表
     */
    private List<TermHint> parseTermsFromJson(String jsonStr) {
        List<TermHint> terms = new ArrayList<>();
        try {
            // 尝试解析为 JSON 数组
            JSONArray jsonArray = JSON.parseArray(jsonStr);
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                TermHint term = new TermHint(
                        item.getString("term"),
                        item.getString("translation"),
                        item.getDoubleValue("timestamp"),
                        item.getString("category")
                );
                terms.add(term);
            }
        } catch (Exception e) {
            logger.error("❌ 术语 JSON 解析失败: {}", jsonStr, e);
            // 返回空列表而不是抛出异常
        }
        logger.info("✓ 提取术语数: {}", terms.size());
        return terms;
    }

    /**
     * 从 JSON 字符串解析诊断报告
     */
    private DiagnosisReport parseDiagnosisFromJson(String jsonStr) {
        DiagnosisReport report = new DiagnosisReport();
        try {
            JSONObject json = JSON.parseObject(jsonStr);
            report.setScore(json.getIntValue("score"));
            report.setAccuracy(json.getString("accuracy"));
            report.setCompleteness(json.getString("completeness"));
            report.setFluency(json.getString("fluency"));
            report.setTerminology(json.getString("terminology"));

            // 解析主要问题列表
            JSONArray issuesArray = json.getJSONArray("main_issues");
            if (issuesArray != null) {
                report.setMainIssues(issuesArray.toList(String.class));
            }

            // 解析改进建议列表
            JSONArray suggestionsArray = json.getJSONArray("suggestions");
            if (suggestionsArray != null) {
                report.setSuggestions(suggestionsArray.toList(String.class));
            }
        } catch (Exception e) {
            logger.error("❌ 诊断报告 JSON 解析失败: {}", jsonStr, e);
        }
        logger.info("✓ 诊断报告生成: 评分 {}", report.getScore());
        return report;
    }
}
