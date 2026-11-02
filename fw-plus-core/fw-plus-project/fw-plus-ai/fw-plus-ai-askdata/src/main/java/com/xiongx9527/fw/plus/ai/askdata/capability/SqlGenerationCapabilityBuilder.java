package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ConfidenceEstimator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPromptBuilder;

/**
 * {@link SqlGenerationCapability} 的流式构建器。至少提供 {@link LlmChatPort}，其余使用默认实现。
 */
public class SqlGenerationCapabilityBuilder {

    private LlmChatPort llm;
    private SqlPromptBuilder promptBuilder;
    private SqlValidator validator;
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

    public SqlGenerationCapabilityBuilder validator(SqlValidator v) {
        this.validator = v;
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
        return new DefaultSqlGenerationCapability(llm, promptBuilder, validator, confidenceEstimator, repairPolicy);
    }
}
