package com.xiongx9527.fw.plus.ai.askdata.prompt;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.jdbc.dialect.DialectHelper;
import com.xiongx9527.fw.plus.jdbc.dialect.SqlDialect;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.List;

/**
 * 根据 {@link SqlGenInput} 构造 NL2SQL 提示词。三个后端共用同一份模板（§6.38.8）。
 */
public class SqlPromptBuilder {

    public SqlPrompt build(SqlGenInput input) {
        SqlDialect dialect = SqlDialect.fromHint(input.getDialectHint());
        String schema = input.getSchemaContext() == null ? "(no schema provided)" : input.getSchemaContext().render();

        StringBuilder constraintsBlock = new StringBuilder();
        if (input.getConstraints() != null && !input.getConstraints().isEmpty()) {
            constraintsBlock.append(I18nMessageResolver.resolveKey("askdata.prompt.sql.constraints_header") + "\n");
            for (String c : input.getConstraints()) {
                constraintsBlock.append("- ").append(c).append("\n");
            }
        }

        StringBuilder priorBlock = new StringBuilder();
        if (input.getPriorSql() != null && !input.getPriorSql().trim().isEmpty()) {
            priorBlock.append(I18nMessageResolver.resolveKey("askdata.prompt.sql.prior_header")).append(input.getPriorSql()).append("\n");
        }

        StringBuilder refBlock = new StringBuilder();
        if (input.getRefResolution() != null && !input.getRefResolution().trim().isEmpty()) {
            refBlock.append(I18nMessageResolver.resolveKey("askdata.prompt.sql.ref_header")).append(input.getRefResolution()).append("\n");
        }

        StringBuilder historyBlock = new StringBuilder();
        List<ChatMessage> history = input.getHistory();
        if (history != null && !history.isEmpty()) {
            historyBlock.append(I18nMessageResolver.resolveKey("askdata.prompt.sql.history_header")).append("\n");
            for (ChatMessage m : history) {
                if (m == null) {
                    continue;
                }
                String roleLabel = m.getRole() == null ? "user"
                        : m.getRole().name().toLowerCase(java.util.Locale.ROOT);
                historyBlock.append(roleLabel).append(": ").append(m.getContent()).append("\n");
            }
        }

        StringBuilder termsBlock = ragBlock("askdata.prompt.sql.terms_header", input.getTerms());
        StringBuilder sqlExamplesBlock = ragBlock("askdata.prompt.sql.examples_header", input.getSqlExamples());
        StringBuilder customPromptBlock = new StringBuilder();
        if (input.getCustomPrompt() != null && !input.getCustomPrompt().trim().isEmpty()) {
            customPromptBlock.append(I18nMessageResolver.resolveKey("askdata.prompt.sql.custom_prompt_header"))
                    .append(input.getCustomPrompt()).append("\n");
        }

        String dialectText = input.getDialectHint() == null || input.getDialectHint().trim().isEmpty()
                ? I18nMessageResolver.resolveKey("askdata.prompt.sql.dialect_unknown")
                : input.getDialectHint() + " " + DialectHelper.paginationHint(dialect);

        String user = SqlPromptTemplates.USER_TEMPLATE
                .replace("{dialect}", dialectText)
                .replace("{schema}", schema)
                .replace("{constraintsBlock}", constraintsBlock.toString())
                .replace("{priorBlock}", priorBlock.toString())
                .replace("{refBlock}", refBlock.toString())
                .replace("{historyBlock}", historyBlock.toString())
                .replace("{termsBlock}", termsBlock.toString())
                .replace("{sqlExamplesBlock}", sqlExamplesBlock.toString())
                .replace("{customPromptBlock}", customPromptBlock.toString())
                .replace("{question}", input.getQuestion());

        return new SqlPrompt(SqlPromptTemplates.SYSTEM, user);
    }

    /** 将召回的 RAG 列表渲染成「{header}条目1\n条目2\n」段落；列表为空时返回空串。 */
    private static StringBuilder ragBlock(String headerKey, List<String> items) {
        StringBuilder sb = new StringBuilder();
        if (items != null && !items.isEmpty()) {
            sb.append(I18nMessageResolver.resolveKey(headerKey)).append("\n");
            for (String item : items) {
                if (item != null && !item.trim().isEmpty()) {
                    sb.append("- ").append(item.trim()).append("\n");
                }
            }
        }
        return sb;
    }
}
