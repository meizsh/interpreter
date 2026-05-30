package com.interpreter.aibackend.entity;

import java.util.List;

/**
 * 学生口译诊断报告。
 */
public class DiagnosisReport {
    private int score;                  // 综合评分，范围 0-100。
    private String accuracy;            // 准确度分析。
    private String completeness;        // 完整性分析。
    private String fluency;             // 流畅性分析。
    private String terminology;         // 术语和数字处理分析。
    private List<String> mainIssues;    // 主要问题列表。
    private List<String> suggestions;   // 改进建议列表。

    public DiagnosisReport() {
        this.score = 0;
        this.mainIssues = List.of();
        this.suggestions = List.of();
    }

    public DiagnosisReport(int score, String accuracy, String completeness, String fluency, String terminology) {
        this.score = score;
        this.accuracy = accuracy;
        this.completeness = completeness;
        this.fluency = fluency;
        this.terminology = terminology;
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getAccuracy() { return accuracy; }
    public void setAccuracy(String accuracy) { this.accuracy = accuracy; }

    public String getCompleteness() { return completeness; }
    public void setCompleteness(String completeness) { this.completeness = completeness; }

    public String getFluency() { return fluency; }
    public void setFluency(String fluency) { this.fluency = fluency; }

    public String getTerminology() { return terminology; }
    public void setTerminology(String terminology) { this.terminology = terminology; }

    public List<String> getMainIssues() { return mainIssues; }
    public void setMainIssues(List<String> mainIssues) { this.mainIssues = mainIssues; }

    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

    @Override
    public String toString() {
        return "DiagnosisReport{" +
                "score=" + score +
                ", accuracy='" + accuracy + '\'' +
                ", completeness='" + completeness + '\'' +
                ", fluency='" + fluency + '\'' +
                ", terminology='" + terminology + '\'' +
                '}';
    }
}
