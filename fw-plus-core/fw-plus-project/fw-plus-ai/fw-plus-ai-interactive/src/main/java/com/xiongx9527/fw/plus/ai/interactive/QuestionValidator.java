package com.xiongx9527.fw.plus.ai.interactive;

import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 答案校验策略（Strategy）：对一道题的回答做格式 / 范围 / 必填校验。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>必填：值为空且 required 时失败（key=interactive.required）；</li>
 *   <li>格式：pattern 非空时，字符串值必须匹配（key=interactive.format）；</li>
 *   <li>范围：min/max 非空时，若值可解析为数字则做数值区间校验，否则按字符串长度校验（key=interactive.range）。</li>
 * </ul>
 */
public final class QuestionValidator {

    public ValidationResult validate(Question question, Object value) {
        if (isBlank(value)) {
            return question.isRequired() ? ValidationResult.invalid("interactive.required")
                    : ValidationResult.valid();
        }
        String text = value instanceof String ? (String) value : String.valueOf(value);
        String pattern = question.getPattern();
        if (pattern != null && !pattern.isEmpty()) {
            if (!matches(pattern, text)) {
                return ValidationResult.invalid("interactive.format");
            }
        }
        Long min = question.getMin();
        Long max = question.getMax();
        if (min != null || max != null) {
            Long numeric = parseLong(text);
            if (numeric != null) {
                if (min != null && numeric < min) {
                    return ValidationResult.invalid("interactive.range");
                }
                if (max != null && numeric > max) {
                    return ValidationResult.invalid("interactive.range");
                }
            } else {
                int len = text.length();
                if (min != null && len < min) {
                    return ValidationResult.invalid("interactive.range");
                }
                if (max != null && len > max) {
                    return ValidationResult.invalid("interactive.range");
                }
            }
        }
        return ValidationResult.valid();
    }

    static boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String) {
            return ((String) value).trim().isEmpty();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).isEmpty();
        }
        return false;
    }

    private static boolean matches(String regex, String text) {
        try {
            return Pattern.compile(regex).matcher(text).find();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private static Long parseLong(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            try {
                return Math.round(Double.parseDouble(text.trim()));
            } catch (NumberFormatException e2) {
                return null;
            }
        }
    }
}
