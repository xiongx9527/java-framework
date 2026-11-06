package com.xiongx9527.fw.plus.ai.askdata.clarify;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.StringJoiner;

/**
 * 默认 {@link ClarificationCapability}（§6.32）：把问题点转述为对用户的澄清问题。
 * 仅在生成无效 / 低置信时调用，避免把可能错误的 SQL 直接执行。
 */
public class DefaultClarificationCapability implements ClarificationCapability {

    private final LlmPort llm;

    public DefaultClarificationCapability(LlmPort llm) {
        if (llm == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.llm_required"));
        }
        this.llm = llm;
    }

    @Override
    public ClarificationResult clarify(ClarificationInput input) {
        if (input == null || input.getQuestion() == null) {
            return ClarificationResult.builder().valid(false).build();
        }
        String userPrompt = buildUserPrompt(input);
        try {
            LlmResponse response = llm.complete(LlmRequest.builder()
                    .addMessage(ChatMessage.system(SYSTEM_PROMPT))
                    .addMessage(ChatMessage.user(userPrompt))
                    .build());
            String raw = response == null ? null : response.getContent();
            String text = raw == null ? null : raw.trim();
            if (text == null || text.isEmpty()) {
                return ClarificationResult.builder().valid(false).build();
            }
            return ClarificationResult.builder().text(text).valid(true).build();
        } catch (RuntimeException e) {
            return ClarificationResult.builder().valid(false).build();
        }
    }

    private String buildUserPrompt(ClarificationInput input) {
        StringJoiner issues = new StringJoiner("；");
        if (input.getIssues() != null) {
            for (String s : input.getIssues()) {
                if (s != null && !s.trim().isEmpty()) {
                    issues.add(s);
                }
            }
        }
        String tables = input.getCandidateTables() == null ? ""
                : String.join(", ", input.getCandidateTables());
        return "用户原始问题：\n" + input.getQuestion() + "\n\n"
                + (issues.length() == 0 ? "" : "当前无法可靠生成 SQL 的原因：\n" + issues + "\n\n")
                + (tables.isEmpty() ? "" : "可见的表有：" + tables + "\n\n")
                + "请向用户提出一个简短的澄清问题（一句话），帮助明确其意图，"
                + "以便重新生成正确的 SQL。只输出问题本身，不要解释。";
    }

    private static final String SYSTEM_PROMPT =
            "你是一个数据问数助手。当用户问题不足以生成可靠 SQL 时，你负责向用户提出澄清问题，"
            + "收敛其意图。措辞用简体中文，保持友好、具体。";
}
