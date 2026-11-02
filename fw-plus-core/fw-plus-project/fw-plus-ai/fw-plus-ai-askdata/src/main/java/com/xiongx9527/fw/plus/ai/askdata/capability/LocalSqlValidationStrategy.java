package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;

/** 本地规则校验策略：复用 {@link SqlValidator} 的 DML 禁用 / 表名可见 / 括号平衡。 */
public class LocalSqlValidationStrategy implements SqlValidationStrategy {
    private final SqlValidator validator;

    public LocalSqlValidationStrategy() {
        this(new SqlValidator());
    }

    public LocalSqlValidationStrategy(SqlValidator validator) {
        this.validator = validator == null ? new SqlValidator() : validator;
    }

    @Override
    public ValidationResult validate(String sql, SqlGenInput input) {
        return validator.validate(sql, input == null ? null : input.getSchemaContext());
    }
}
