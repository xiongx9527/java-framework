package com.xiongx9527.fw.plus.ai.askdata.prompt;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.dialect.DialectHelper;
import com.xiongx9527.fw.plus.ai.askdata.dialect.SqlDialect;

/**
 * 根据 {@link SqlGenInput} 构造 NL2SQL 提示词。三个后端共用同一份模板（§6.38.8）。
 */
public class SqlPromptBuilder {

    public SqlPrompt build(SqlGenInput input) {
        SqlDialect dialect = SqlDialect.fromHint(input.getDialectHint());
        String schema = input.getSchemaContext() == null ? "(no schema provided)" : input.getSchemaContext().render();

        StringBuilder constraintsBlock = new StringBuilder();
        if (input.getConstraints() != null && !input.getConstraints().isEmpty()) {
            constraintsBlock.append("额外约束：\n");
            for (String c : input.getConstraints()) {
                constraintsBlock.append("- ").append(c).append("\n");
            }
        }

        StringBuilder priorBlock = new StringBuilder();
        if (input.getPriorSql() != null && !input.getPriorSql().trim().isEmpty()) {
            priorBlock.append("上轮 SQL 候选：").append(input.getPriorSql()).append("\n");
        }

        StringBuilder refBlock = new StringBuilder();
        if (input.getRefResolution() != null && !input.getRefResolution().trim().isEmpty()) {
            refBlock.append("指代消解：").append(input.getRefResolution()).append("\n");
        }

        String dialectText = input.getDialectHint() == null || input.getDialectHint().trim().isEmpty()
                ? "未指定（请使用通用 SQL-92）"
                : input.getDialectHint() + "。" + DialectHelper.paginationHint(dialect);

        String user = SqlPromptTemplates.USER_TEMPLATE
                .replace("{dialect}", dialectText)
                .replace("{schema}", schema)
                .replace("{constraintsBlock}", constraintsBlock.toString())
                .replace("{priorBlock}", priorBlock.toString())
                .replace("{refBlock}", refBlock.toString())
                .replace("{question}", input.getQuestion());

        return new SqlPrompt(SqlPromptTemplates.SYSTEM, user);
    }
}
