package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ConfidenceEstimator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPromptBuilder;

/**
 * {@link SqlGenerationCapability} 的流式构建器。至少提供 {@link LlmChatPort}；
 * LLM 语义校验默认开启（本地优先 + LLM 组合），可用 {@link #llmValidation} 覆盖或关闭。
 */
public class SqlGenerationCapabilityBuilder {
    private LlmChatPort llm;
    private SqlPromptBuilder promptBuilder;
    private SqlValidationStrategy localValidation;
    private SqlValidationStrategy llmValidation;
    private ConfidenceEstimator confidenceEstimator;
    private SqlRepairPolicy repairPolicy;

    public static SqlGenerationCapabilityBuilder builder() {
        return new SqlGenerationCapabilityBuilder();
    }

    public SqlGenerationCapabilityBuilder llm(LlmChatPort v) {
        this.llm = v;
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

    public SqlGenerationCapability build() {
        if (llm == null) {
            throw new IllegalStateException("llm is required to build SqlGenerationCapability");
        }
        SqlValidationStrategy local = localValidation == null ? new LocalSqlValidationStrategy() : localValidation;
        SqlValidationStrategy llmV = llmValidation;
        if (llmV == null) {
            llmV = new LlmSqlValidationStrategy(llm, local);
        }
        return new DefaultSqlGenerationCapability(llm, promptBuilder, local, llmV, confidenceEstimator, repairPolicy);
    }
}
