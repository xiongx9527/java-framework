package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;

/** SQL 校验策略（§6.14 L1 护栏）。可串联：本地规则 → LLM 语义校验。 */
public interface SqlValidationStrategy {
    ValidationResult validate(String sql, SqlGenInput input);
}
