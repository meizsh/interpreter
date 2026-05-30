package com.interpreter.aibackend.entity;

/**
 * 实时术语提示卡片。
 */
public class TermHint {
    private String term;          // 源语中的术语或短语。
    private String translation;   // 目标语参考译法。
    private Double timestamp;     // 预计出现时间，单位：秒。
    private String category;      // 分类：术语、人名、数字、机构、短语等。

    public TermHint() {}

    public TermHint(String term, String translation, Double timestamp) {
        this.term = term;
        this.translation = translation;
        this.timestamp = timestamp;
    }

    public TermHint(String term, String translation, Double timestamp, String category) {
        this.term = term;
        this.translation = translation;
        this.timestamp = timestamp;
        this.category = category;
    }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getTranslation() { return translation; }
    public void setTranslation(String translation) { this.translation = translation; }

    public Double getTimestamp() { return timestamp; }
    public void setTimestamp(Double timestamp) { this.timestamp = timestamp; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "TermHint{" +
                "term='" + term + '\'' +
                ", translation='" + translation + '\'' +
                ", timestamp=" + timestamp +
                ", category='" + category + '\'' +
                '}';
    }
}
