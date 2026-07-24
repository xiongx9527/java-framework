package com.xiongx9527.fw.plus.ai.askdata.prompt;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRetrievedDocument;

import java.util.Collections;
import java.util.List;

public class DefaultAskDataPromptTemplateProvider implements AskDataPromptTemplateProvider {

    private final AskDataDialectRegistry dialectRegistry;

    public DefaultAskDataPromptTemplateProvider(AskDataDialectRegistry dialectRegistry) {
        this.dialectRegistry = dialectRegistry;
    }

    @Override
    public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
            List<AskDataRetrievedDocument> retrievedDocuments, int maxRows) {
        String dialectName = request == null ? null : request.getDialect();
        if (isBlank(dialectName) && metadata != null) {
            dialectName = metadata.getDialect();
        }
        AskDataDialect dialect = dialectRegistry.getDialect(dialectName);
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 AskData 数据分析助手。仅输出 JSON，对象必须包含键 ")
                .append("\"sql\", \"tables\", \"chartType\", \"brief\"。\n")
                .append("SQL 方言: ").append(dialect.getName()).append("。")
                .append(dialect.getIdentifierRule()).append("\n")
                .append("只生成只读 SELECT 或 WITH 查询，且只能生成单条 SQL 语句。\n")
                .append(dialect.getLimitRule()).append(" SQL 必须包含 LIMIT ").append(maxRows).append("。\n");
        if (request != null && request.getQuestion() != null) {
            prompt.append("问题: ").append(request.getQuestion()).append("\n");
        }
        appendMetadata(prompt, metadata);
        appendDocuments(prompt, retrievedDocuments);
        return prompt.toString();
    }

    private void appendMetadata(StringBuilder prompt, AskDataMetadata metadata) {
        if (metadata == null) {
            return;
        }
        prompt.append("Schema:\n");
        for (AskDataTable table : metadata.getTables()) {
            prompt.append(table.getName()).append("(");
            for (int i = 0; i < table.getColumns().size(); i++) {
                AskDataColumn column = table.getColumns().get(i);
                if (i > 0) { prompt.append(", "); }
                prompt.append(column.getName()).append(" ").append(column.getType());
            }
            prompt.append(")\n");
        }
        appendLines(prompt, "术语", metadata.getTerms());
        appendLines(prompt, "样例", metadata.getExamples());
        appendLines(prompt, "补充规则", metadata.getPromptFragments());
    }

    private void appendDocuments(StringBuilder prompt, List<AskDataRetrievedDocument> documents) {
        List<AskDataRetrievedDocument> values = documents == null ? Collections.<AskDataRetrievedDocument>emptyList() : documents;
        if (!values.isEmpty()) {
            prompt.append("向量召回:\n");
            for (AskDataRetrievedDocument document : values) {
                if (document != null && document.getContent() != null) {
                    prompt.append(document.getContent()).append("\n");
                }
            }
        }
    }

    private void appendLines(StringBuilder prompt, String title, List<String> values) {
        for (String value : values) {
            prompt.append(title).append(": ").append(value).append("\n");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
