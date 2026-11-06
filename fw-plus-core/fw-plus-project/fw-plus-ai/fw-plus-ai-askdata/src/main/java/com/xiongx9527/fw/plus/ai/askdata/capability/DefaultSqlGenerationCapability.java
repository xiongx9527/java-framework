package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ConfidenceEstimator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlExtractor;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPrompt;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPromptBuilder;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认 {@link SqlGenerationCapability} 实现（§6.38）：构造提示词 → 调用 LLM → 提取 SQL →
 * （本地优先 + LLM 语义）校验 →（可选）自我修复 → 估置信度。
 *
 * <p>校验为本地优先的两层组合：本地规则（DML 禁用 / 表名可见 / 括号平衡）先过，通过后再跑
 * 可选的 LLM 语义校验；任一层不过即进入修复环（§6.17 SQL 修复环），由 LLM 带着失败原因重出 SQL。
 * 修复环默认 fail-closed（§6.20 / P0 #6）：修复后仍不通过则抛 {@link SqlGenerationException}，
 * 不把可能错误的 SQL 交给下游执行。</p>
 */
public class DefaultSqlGenerationCapability implements SqlGenerationCapability {

    private final LlmPort llm;
    private final SqlPromptBuilder promptBuilder;
    private final SqlValidationStrategy localValidation;
    private final SqlValidationStrategy llmValidation;
    private final ConfidenceEstimator confidenceEstimator;
    private final SqlRepairPolicy repairPolicy;
    private final boolean failClosed;
    private final boolean normalizeInput;

    public DefaultSqlGenerationCapability(LlmPort llm) {
        this(llm, new SqlPromptBuilder(), new LocalSqlValidationStrategy(), null,
                new ConfidenceEstimator(), SqlRepairPolicy.builder().build(), false);
    }

    public DefaultSqlGenerationCapability(LlmPort llm, SqlPromptBuilder promptBuilder,
                                         SqlValidationStrategy localValidation,
                                         SqlValidationStrategy llmValidation,
                                         ConfidenceEstimator confidenceEstimator,
                                         SqlRepairPolicy repairPolicy, boolean normalizeInput) {
        if (llm == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.llm_required"));
        }
        this.llm = llm;
        this.promptBuilder = promptBuilder == null ? new SqlPromptBuilder() : promptBuilder;
        this.localValidation = localValidation == null ? new LocalSqlValidationStrategy() : localValidation;
        this.llmValidation = llmValidation;
        this.confidenceEstimator = confidenceEstimator == null ? new ConfidenceEstimator() : confidenceEstimator;
        this.repairPolicy = repairPolicy;
        this.failClosed = repairPolicy != null ? repairPolicy.isFailClosed() : true;
        this.normalizeInput = normalizeInput;
    }

    @Override
    public SqlGenResult generate(SqlGenInput input) {
        if (input == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.input_null"));
        }
        if (normalizeInput) {
            input = SqlGenInputNormalizer.normalize(input);
        }
        input.validate();
        SqlPrompt prompt = promptBuilder.build(input);
        String raw = safeComplete(prompt, input);
        String sql = SqlExtractor.extract(raw);
        boolean repaired = false;
        List<String> alternatives = new ArrayList<String>();
        ValidationResult vr = validateChain(sql, input);
        if (!vr.isOk() && repairPolicy != null && repairPolicy.isEnabled()) {
            int attempts = Math.max(0, repairPolicy.getMaxAttempts());
            for (int i = 0; i < attempts && !vr.isOk(); i++) {
                String repairUser = prompt.getUserPrompt()
                        + System.lineSeparator() + I18nMessageResolver.resolveKey("askdata.prompt.sql.repair_prefix") + joinIssues(vr)
                        + I18nMessageResolver.resolveKey("askdata.prompt.sql.repair_suffix");
                raw = safeComplete(new SqlPrompt(prompt.getSystemPrompt(), repairUser), input);
                sql = SqlExtractor.extract(raw);
                vr = validateChain(sql, input);
                if (vr.isOk()) {
                    break;
                }
                alternatives.add(sql);
                repaired = true;
            }
        }
        if (!vr.isOk() && failClosed) {
            throw new SqlGenerationException(50005806, joinIssues(vr));
        }
        double confidence = confidenceEstimator.estimate(vr, repaired);
        String explanation = SqlExtractor.extractExplanation(raw);
        return SqlGenResult.builder()
                .sql(sql)
                .explanation(explanation)
                .referencedTables(vr.getReferencedTables())
                .confidence(confidence)
                .alternatives(alternatives)
                .valid(vr.isOk())
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

    private String safeComplete(SqlPrompt p, SqlGenInput input) {
        try {
            LlmRequest.Builder rb = LlmRequest.builder()
                    .addMessage(ChatMessage.system(p.getSystemPrompt()))
                    .addMessage(ChatMessage.user(p.getUserPrompt()));
            if (input.getSeed() != null) {
                rb.seed(input.getSeed());
            }
            if (input.getTemperature() != null) {
                rb.temperature(input.getTemperature());
            }
            if (input.getTopP() != null) {
                rb.topP(input.getTopP());
            }
            if (input.getExtraParams() != null && !input.getExtraParams().isEmpty()) {
                rb.extraParams(input.getExtraParams());
            }
            LlmResponse response = llm.complete(rb.build());
            return response == null ? null : response.getContent();
        } catch (Exception e) {
            throw new SqlGenerationException(50005801, I18nMessageResolver.resolveKey("askdata.gen.llm_failed"), e);
        }
    }

    /**
     * 流式生成（§6.38 流式输出）：直接返回底层 {@link LlmTokenStream}，调用方逐 token 消费。
     * 与 {@link #generate(SqlGenInput)} 共用同一套提示词构造，但不做抽取 / 校验 / 修复，仅透传原始流。
     *
     * <p>注意：此为 {@link DefaultSqlGenerationCapability} 的具体扩展方法，未纳入
     * {@link SqlGenerationCapability} 接口（避免破坏既有实现），需要流式时直接使用本实现类。</p>
     */
    public LlmTokenStream generateStream(SqlGenInput input) {
        if (input == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.input_null"));
        }
        if (normalizeInput) {
            input = SqlGenInputNormalizer.normalize(input);
        }
        SqlPrompt prompt = promptBuilder.build(input);
        LlmRequest.Builder rb = LlmRequest.builder()
                .addMessage(ChatMessage.system(prompt.getSystemPrompt()))
                .addMessage(ChatMessage.user(prompt.getUserPrompt()));
        if (input.getSeed() != null) {
            rb.seed(input.getSeed());
        }
        if (input.getTemperature() != null) {
            rb.temperature(input.getTemperature());
        }
        if (input.getTopP() != null) {
            rb.topP(input.getTopP());
        }
        if (input.getExtraParams() != null && !input.getExtraParams().isEmpty()) {
            rb.extraParams(input.getExtraParams());
        }
        return llm.stream(rb.build());
    }

    private static String joinIssues(ValidationResult vr) {
        StringJoiner sj = new StringJoiner("; ");
        for (String s : vr.getIssues()) {
            sj.add(s);
        }
        return sj.toString();
    }
}
