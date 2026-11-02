package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ConfidenceEstimator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlExtractor;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPrompt;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPromptBuilder;
import java.util.StringJoiner;

/**
 * 默认 {@link SqlGenerationCapability} 实现（§6.38）：构造提示词 → 调用 LLM → 提取 SQL →
 * 验证 →（可选）自我修复 → 估置信度。三个后端共用同一份逻辑，只有 LLM 调用不同。
 */
public class DefaultSqlGenerationCapability implements SqlGenerationCapability {

    private final LlmChatPort llm;
    private final SqlPromptBuilder promptBuilder;
    private final SqlValidator validator;
    private final ConfidenceEstimator confidenceEstimator;
    private final SqlRepairPolicy repairPolicy;

    public DefaultSqlGenerationCapability(LlmChatPort llm, SqlPromptBuilder promptBuilder,
                                         SqlValidator validator, ConfidenceEstimator confidenceEstimator,
                                         SqlRepairPolicy repairPolicy) {
        if (llm == null) {
            throw new IllegalArgumentException("llm is required");
        }
        this.llm = llm;
        this.promptBuilder = promptBuilder == null ? new SqlPromptBuilder() : promptBuilder;
        this.validator = validator == null ? new SqlValidator() : validator;
        this.confidenceEstimator = confidenceEstimator == null ? new ConfidenceEstimator() : confidenceEstimator;
        this.repairPolicy = repairPolicy;
    }

    @Override
    public SqlGenResult generate(SqlGenInput input) {
        if (input == null) {
            throw new SqlGenerationException("input is null");
        }
        input.validate();
        SqlPrompt prompt = promptBuilder.build(input);
        String raw = safeComplete(prompt);
        String sql = SqlExtractor.extract(raw);
        boolean repaired = false;
        ValidationResult vr = validator.validate(sql, input.getSchemaContext());
        if (!vr.isOk() && repairPolicy != null && repairPolicy.isEnabled()) {
            int attempts = Math.max(0, repairPolicy.getMaxAttempts());
            for (int i = 0; i < attempts && !vr.isOk(); i++) {
                String repairUser = prompt.getUserPrompt()
                        + "\n上次生成的 SQL 验证失败：" + joinIssues(vr) + "\n请修正后仅重新输出 SQL。";
                raw = safeComplete(new SqlPrompt(prompt.getSystemPrompt(), repairUser));
                sql = SqlExtractor.extract(raw);
                vr = validator.validate(sql, input.getSchemaContext());
                repaired = true;
            }
        }
        double confidence = confidenceEstimator.estimate(vr, repaired);
        String explanation = SqlExtractor.extractExplanation(raw);
        return SqlGenResult.builder()
                .sql(sql)
                .explanation(explanation)
                .referencedTables(vr.getReferencedTables())
                .confidence(confidence)
                .build();
    }

    private String safeComplete(SqlPrompt p) {
        try {
            return llm.complete(p.getSystemPrompt(), p.getUserPrompt());
        } catch (Exception e) {
            throw new SqlGenerationException("llm call failed", e);
        }
    }

    private static String joinIssues(ValidationResult vr) {
        StringJoiner sj = new StringJoiner("; ");
        for (String s : vr.getIssues()) {
            sj.add(s);
        }
        return sj.toString();
    }
}
