package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ConfidenceEstimator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlExtractor;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPrompt;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPromptBuilder;
import java.util.StringJoiner;

/**
 * 默认 {@link SqlGenerationCapability} 实现（§6.38）：构造提示词 → 调用 LLM → 提取 SQL →
 * （本地优先 + LLM 语义）校验 →（可选）自我修复 → 估置信度。
 *
 * <p>校验为本地优先的两层组合：本地规则（DML 禁用 / 表名可见 / 括号平衡）先过，通过后再跑
 * 可选的 LLM 语义校验；任一层不过即进入修复环（§6.17 SQL 修复环），由 LLM 带着失败原因重出 SQL。</p>
 */
public class DefaultSqlGenerationCapability implements SqlGenerationCapability {

    private final LlmChatPort llm;
    private final SqlPromptBuilder promptBuilder;
    private final SqlValidationStrategy localValidation;
    private final SqlValidationStrategy llmValidation;
    private final ConfidenceEstimator confidenceEstimator;
    private final SqlRepairPolicy repairPolicy;

    public DefaultSqlGenerationCapability(LlmChatPort llm) {
        this(llm, new SqlPromptBuilder(), new LocalSqlValidationStrategy(), null,
                new ConfidenceEstimator(), new SqlRepairPolicy());
    }

    public DefaultSqlGenerationCapability(LlmChatPort llm, SqlPromptBuilder promptBuilder,
                                         SqlValidationStrategy localValidation,
                                         SqlValidationStrategy llmValidation,
                                         ConfidenceEstimator confidenceEstimator,
                                         SqlRepairPolicy repairPolicy) {
        if (llm == null) {
            throw new IllegalArgumentException("llm is required");
        }
        this.llm = llm;
        this.promptBuilder = promptBuilder == null ? new SqlPromptBuilder() : promptBuilder;
        this.localValidation = localValidation == null ? new LocalSqlValidationStrategy() : localValidation;
        this.llmValidation = llmValidation;
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
        ValidationResult vr = validateChain(sql, input);
        if (!vr.isOk() && repairPolicy != null && repairPolicy.isEnabled()) {
            int attempts = Math.max(0, repairPolicy.getMaxAttempts());
            for (int i = 0; i < attempts && !vr.isOk(); i++) {
                String repairUser = prompt.getUserPrompt()
                        + System.lineSeparator() + "上次生成的 SQL 校验失败：" + joinIssues(vr)
                        + "。请修正后仅重新输出 SQL。";
                raw = safeComplete(new SqlPrompt(prompt.getSystemPrompt(), repairUser));
                sql = SqlExtractor.extract(raw);
                vr = validateChain(sql, input);
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

    /** 本地优先：本地规则先过，通过后（若配置了）再跑 LLM 语义校验（§6.14 L1 + §6.38）。 */
    private ValidationResult validateChain(String sql, SqlGenInput input) {
        ValidationResult local = localValidation.validate(sql, input);
        if (!local.isOk()) {
            return local;
        }
        if (llmValidation != null) {
            ValidationResult llmVr = llmValidation.validate(sql, input);
            if (!llmVr.isOk()) {
                return llmVr;
            }
        }
        return local;
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
