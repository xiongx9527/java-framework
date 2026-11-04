package com.xiongx9527.fw.plus.ai.interactive;

/**
 * 答案校验结果：{@code valid} 为假时带一个 i18n key（或兜底描述），用于反问时给出具体原因。
 */
public final class ValidationResult {

    private final boolean valid;
    private final String key;

    private ValidationResult(boolean valid, String key) {
        this.valid = valid;
        this.key = key;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult invalid(String key) {
        return new ValidationResult(false, key);
    }

    public boolean isValid() {
        return valid;
    }

    public String getKey() {
        return key;
    }
}
