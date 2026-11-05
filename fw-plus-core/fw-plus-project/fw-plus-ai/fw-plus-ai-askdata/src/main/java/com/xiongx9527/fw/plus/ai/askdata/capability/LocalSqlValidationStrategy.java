package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.jdbc.dialect.DialectSqlChecker;
import com.xiongx9527.fw.plus.jdbc.dialect.SqlDialect;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import java.util.ArrayList;
import java.util.List;

/** 本地规则校验策略：复用 {@link SqlValidator} 的 DML 禁用 / 表名可见 / 括号平衡，并叠加方言感知检查。 */
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
        ValidationResult local = validator.validate(sql, input == null ? null : input.getSchemaContext());
        if (input == null || input.getDialectHint() == null || input.getDialectHint().trim().isEmpty()) {
            return local;
        }
        SqlDialect dialect = SqlDialect.fromHint(input.getDialectHint());
        if (dialect == SqlDialect.UNKNOWN) {
            return local;
        }
        List<String> dialectIssues = DialectSqlChecker.check(sql, dialect);
        if (dialectIssues.isEmpty()) {
            return local;
        }
        List<String> merged = new ArrayList<>(local.getIssues());
        merged.addAll(dialectIssues);
        return ValidationResult.fail(merged, local.getReferencedTables());
    }
}
