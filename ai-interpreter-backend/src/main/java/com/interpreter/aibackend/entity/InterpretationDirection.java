package com.interpreter.aibackend.entity;

public enum InterpretationDirection {
    EN_ZH("en-zh", "English", "Chinese", 1737, 1537),
    ZH_EN("zh-en", "Chinese", "English", 1537, 1737);

    private final String code;
    private final String sourceLanguage;
    private final String targetLanguage;
    private final int sourceAsrDevPid;
    private final int targetAsrDevPid;

    InterpretationDirection(String code, String sourceLanguage, String targetLanguage, int sourceAsrDevPid, int targetAsrDevPid) {
        this.code = code;
        this.sourceLanguage = sourceLanguage;
        this.targetLanguage = targetLanguage;
        this.sourceAsrDevPid = sourceAsrDevPid;
        this.targetAsrDevPid = targetAsrDevPid;
    }

    public String getCode() {
        return code;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public int getSourceAsrDevPid() {
        return sourceAsrDevPid;
    }

    public int getTargetAsrDevPid() {
        return targetAsrDevPid;
    }

    public static InterpretationDirection fromCode(String code) {
        if (code == null || code.isBlank()) {
            return EN_ZH;
        }
        for (InterpretationDirection direction : values()) {
            if (direction.code.equalsIgnoreCase(code.trim())) {
                return direction;
            }
        }
        return EN_ZH;
    }
}
