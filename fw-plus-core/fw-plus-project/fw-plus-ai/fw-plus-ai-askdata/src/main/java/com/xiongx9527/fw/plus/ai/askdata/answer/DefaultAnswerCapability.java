package com.xiongx9527.fw.plus.ai.askdata.answer;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.jdbc.execution.CsvExporter;
import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认 {@link AnswerCapability}（§6.38）：把结果集收敛为 CSV 片段（截断防超长）→ 调用 LLM →
 * 抽取自然语言回答。结果过大时只取前 {@code maxCsvRows} 行作为依据，并在回答中明确“基于前 N 行”。
 */
public class DefaultAnswerCapability implements AnswerCapability {

    /** 喂给 LLM 的结果行上限，避免提示词爆炸。 */
    private static final int MAX_CSV_ROWS = 50;

    private final LlmPort llm;
    private final int maxCsvRows;

    public DefaultAnswerCapability(LlmPort llm) {
        this(llm, MAX_CSV_ROWS);
    }

    public DefaultAnswerCapability(LlmPort llm, int maxCsvRows) {
        if (llm == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.llm_required"));
        }
        this.llm = llm;
        this.maxCsvRows = maxCsvRows > 0 ? maxCsvRows : MAX_CSV_ROWS;
    }

    @Override
    public AnswerResult answer(AnswerInput input) {
        if (input == null || input.getQuestion() == null) {
            return AnswerResult.builder().valid(false).build();
        }
        String dataSnippet = toDataSnippet(input.getQueryResult());
        String userPrompt = buildUserPrompt(input.getQuestion(), input.getSql(), dataSnippet);
        try {
            LlmResponse response = llm.complete(LlmRequest.builder()
                    .addMessage(ChatMessage.system(SYSTEM_PROMPT))
                    .addMessage(ChatMessage.user(userPrompt))
                    .build());
            String raw = response == null ? null : response.getContent();
            String text = raw == null ? null : raw.trim();
            if (text == null || text.isEmpty()) {
                return AnswerResult.builder().valid(false).build();
            }
            return AnswerResult.builder().text(text).confidence(0.8d).valid(true).build();
        } catch (RuntimeException e) {
            return AnswerResult.builder().valid(false).build();
        }
    }

    private String toDataSnippet(QueryResult qr) {
        if (qr == null || qr.rowCount() == 0) {
            return "(无结果行)";
        }
        String csv = CsvExporter.export(qr);
        String[] lines = csv.split("\\r?\\n");
        int keep = Math.min(lines.length, maxCsvRows + 1); // +1 表头
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keep; i++) {
            sb.append(lines[i]).append('\n');
        }
        if (lines.length > keep) {
            sb.append("... (仅展示前 ").append(maxCsvRows).append(" 行，共 ").append(qr.rowCount()).append(" 行)");
        }
        return sb.toString();
    }

    private String buildUserPrompt(String question, String sql, String dataSnippet) {
        return "用户问题：\n" + question + "\n\n"
                + "使用的 SQL：\n" + (sql == null ? "(无)" : sql) + "\n\n"
                + "查询结果（CSV，首行为列名）：\n" + dataSnippet + "\n\n"
                + "请基于以上结果，用简体中文给出直接回答用户问题的自然语言结论，"
                + "包含关键数值与明显趋势；若结果不足以回答，请如实说明。控制在 3 句话以内。";
    }

    private static final String SYSTEM_PROMPT =
            "你是一个数据分析助手，负责把 SQL 查询结果转述为对用户的自然语言回答。"
            + "只陈述数据能支撑的结论，不编造数字，不输出 SQL。";
}
