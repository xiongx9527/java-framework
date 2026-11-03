package com.xiongx9527.fw.plus.ai.askdata.capability;

import java.util.function.Function;

/**
 * {@link SqlGenInput} 归一（§6.22）：折叠空白、长度上限、可注入前置改写（如相对时间锚定）。
 *
 * <p>只做安全、确定性的归一，不擅自改写语义：
 * <ul>
 *   <li>空白折叠：所有连续空白（含换行/制表）折叠为单个空格并去首尾；</li>
 *   <li>长度上限：超过 {@code maxQuestionLength} 直接失败（fail-fast，不静默截断丢语义）；</li>
 *   <li>前置改写：可选 {@code preprocessor}，由产品侧注入相对时间（今天/上周…）锚定等，
 *       模块本身不内置任何语言相关的日期解析，保持后端无关。</li>
 * </ul>
 */
public final class SqlGenInputNormalizer {

    /** 默认问题长度上限。 */
    public static final int DEFAULT_MAX_LENGTH = 2000;

    private SqlGenInputNormalizer() {
    }

    /** 使用默认长度上限、无前置改写做归一。 */
    public static SqlGenInput normalize(SqlGenInput input) {
        return normalize(input, DEFAULT_MAX_LENGTH, null);
    }

    /**
     * 归一。
     *
     * @param input          原输入（不可为 null）
     * @param maxQuestionLength 问题长度上限，<=0 表示不限制
     * @param preprocessor   前置改写（可为 null，表示恒等）
     */
    public static SqlGenInput normalize(SqlGenInput input, int maxQuestionLength,
                                        Function<String, String> preprocessor) {
        if (input == null) {
            throw new IllegalArgumentException("input is null");
        }
        String q = input.getQuestion();
        if (q == null) {
            return input;
        }
        if (preprocessor != null) {
            q = preprocessor.apply(q);
        }
        if (q != null) {
            q = q.replaceAll("\\s+", " ").trim();
        }
        if (maxQuestionLength > 0 && q != null && q.length() > maxQuestionLength) {
            throw new IllegalArgumentException("question 超过长度上限 " + maxQuestionLength
                    + "，当前 " + q.length() + " 字符");
        }
        return SqlGenInput.builder()
                .question(q)
                .schemaContext(input.getSchemaContext())
                .dialectHint(input.getDialectHint())
                .priorSql(input.getPriorSql())
                .refResolution(input.getRefResolution())
                .constraints(input.getConstraints())
                .build();
    }
}
