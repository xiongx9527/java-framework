package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.ResilientLlmPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ConfidenceEstimator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPromptBuilder;

/**
 * {@link SqlGenerationCapability} 的流式构建器。至少提供 {@link LlmPort}；
 * LLM 语义校验默认开启（本地优先 + LLM 组合），可用 {@link #llmValidation} 覆盖或关闭。
 * 可选 {@link #resilient} 让生成与 LLM 校验共用同一个韧性端口（重试 + 熔断，§6.26 / D.4）。
 */
public class SqlGenerationCapabilityBuilder {
    private LlmPort llm;
    private SqlPromptBuilder promptBuilder;
    private SqlValidationStrategy localValidation;
    private SqlValidationStrategy llmValidation;
    private ConfidenceEstimator confidenceEstimator;
    private SqlRepairPolicy repairPolicy;
    private boolean failClosed = true;
    private boolean normalizeInput = false;
    private ResilientLlmPort.ResilienceConfig resilienceConfig;

    public static SqlGenerationCapabilityBuilder builder() {
        return new SqlGenerationCapabilityBuilder();
    }

    public SqlGenerationCapabilityBuilder llm(LlmPort v) {
        this.llm = v;
        return this;
    }

    /** 开启 LLM 韧性（重试 + 熔断）；null 表示不包裹。生成与 LLM 校验共用被包裹后的端口。 */
    public SqlGenerationCapabilityBuilder resilient(ResilientLlmPort.ResilienceConfig v) {
        this.resilienceConfig = v;
        return this;
    }

    public SqlGenerationCapabilityBuilder promptBuilder(SqlPromptBuilder v) {
        this.promptBuilder = v;
        return this;
    }

    public SqlGenerationCapabilityBuilder localValidation(SqlValidationStrategy v) {
        this.localValidation = v;
        return this;
    }

    public SqlGenerationCapabilityBuilder llmValidation(SqlValidationStrategy v) {
        this.llmValidation = v;
        return this;
    }

    public SqlGenerationCapabilityBuilder confidenceEstimator(ConfidenceEstimator v) {
        this.confidenceEstimator = v;
        return this;
    }

    public SqlGenerationCapabilityBuilder repairPolicy(SqlRepairPolicy v) {
        this.repairPolicy = v;
        return this;
    }

    public SqlGenerationCapabilityBuilder failClosed(boolean v) {
        this.failClosed = v;
        return this;
    }

    /** 是否在生成前对输入做 §6.22 归一（空白折叠 + 长度上限）。默认 false。 */
    public SqlGenerationCapabilityBuilder normalizeInput(boolean v) {
        this.normalizeInput = v;
        return this;
    }

    public SqlGenerationCapability build() {
        if (llm == null) {
            throw new IllegalStateException(I18nMessageResolver.resolveKey("askdata.contract.build_llm"));
        }
        LlmPort effectiveLlm = llm;
        if (resilienceConfig != null) {
            effectiveLlm = new ResilientLlmPort(llm, resilienceConfig);
        }
        SqlValidationStrategy local = localValidation == null ? new LocalSqlValidationStrategy() : localValidation;
        SqlValidationStrategy llmV = llmValidation;
        if (llmV == null) {
            llmV = new LlmSqlValidationStrategy(effectiveLlm, local);
        }
        // failClosed 单一来源：以 repairPolicy 为准；构建器上的 .failClosed(x) 优先生效。
        SqlRepairPolicy rp = repairPolicy;
        if (rp == null) {
            rp = SqlRepairPolicy.builder().failClosed(failClosed).build();
        } else if (rp.isFailClosed() != failClosed) {
            rp = SqlRepairPolicy.builder()
                    .enabled(rp.isEnabled())
                    .maxAttempts(rp.getMaxAttempts())
                    .failClosed(failClosed)
                    .build();
        }
        return new DefaultSqlGenerationCapability(effectiveLlm, promptBuilder, local, llmV,
                confidenceEstimator, rp, normalizeInput);
    }
}
