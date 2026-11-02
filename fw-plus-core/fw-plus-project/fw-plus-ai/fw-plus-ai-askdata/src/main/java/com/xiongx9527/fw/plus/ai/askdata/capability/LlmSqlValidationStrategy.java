package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LLM 语义校验策略（§6.14 L1 + §6.38）：本地规则通过后，再让模型判断 SQL 是否语义正确、
 * 能否回答问题。模型调用失败时回退到本地策略，避免阻断生成链路。
 */
public class LlmSqlValidationStrategy implements SqlValidationStrategy {
    private final LlmChatPort llm;
    private final SqlValidationStrategy fallback;

    public LlmSqlValidationStrategy(LlmChatPort llm) {
        this(llm, new LocalSqlValidationStrategy());
    }

    public LlmSqlValidationStrategy(LlmChatPort llm, SqlValidationStrategy fallback) {
        if (llm == null) {
            throw new IllegalArgumentException("llm is required");
        }
        this.llm = llm;
        this.fallback = fallback == null ? new LocalSqlValidationStrategy() : fallback;
    }

    @Override
    public ValidationResult validate(String sql, SqlGenInput input) {
        String prompt = buildPrompt(sql, input);
        String resp;
        try {
            resp = llm.complete(SqlPromptTemplates.VALIDATION_SYSTEM, prompt);
        } catch (RuntimeException e) {
            return fallback.validate(sql, input);
        }
        return parse(resp);
    }

    private String buildPrompt(String sql, SqlGenInput input) {
        String schema = (input != null && input.getSchemaContext() != null)
                ? input.getSchemaContext().render() : "(no schema)";
        String question = (input != null && input.getQuestion() != null) ? input.getQuestion() : "";
        return "问题：" + question + System.lineSeparator()
                + "可见表结构：" + System.lineSeparator() + schema + System.lineSeparator()
                + "待校验 SQL：" + System.lineSeparator() + sql + System.lineSeparator()
                + "请判断该 SQL 是否语义正确、能否回答上述问题。仅回答 YES 或 NO，"
                + "若 NO 请另起一行给出简短原因。";
    }

    private ValidationResult parse(String resp) {
        if (resp == null) {
            return ValidationResult.ok(Collections.emptyList());
        }
        String head = resp.trim();
        if (head.toUpperCase().startsWith("NO")) {
            int nl = head.indexOf(System.lineSeparator());
            if (nl < 0) {
                nl = head.indexOf("
");
            }
            String reason = nl > 0 ? head.substring(nl).trim() : "llm flagged sql as invalid";
            if (reason.isEmpty()) {
                reason = "llm flagged sql as invalid";
            }
            List<String> issues = new ArrayList<>();
            issues.add("llm: " + reason);
            return ValidationResult.fail(issues, Collections.emptyList());
        }
        return ValidationResult.ok(Collections.emptyList());
    }
}
