package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentMatch;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 意图分类：把候选意图交给模型判断，返回其选中的 ref。
 *
 * <p>适合多意图、需消歧或依赖上下文的场景；结果是「二选一」置信度（命中 1.0 / 未命中 0.0），
 * 不提供连续分数。</p>
 */
public final class LlmIntentClassifier implements IntentPort {

    private final LlmPort llm;

    public LlmIntentClassifier(LlmPort llm) {
        if (llm == null) {
            throw new IllegalArgumentException("llm port is required");
        }
        this.llm = llm;
    }

    @Override
    public IntentResult classify(IntentRequest request) {
        List<IntentMatch> matches = new ArrayList<IntentMatch>();
        if (request == null || request.getCandidates().isEmpty()) {
            return IntentResult.of(matches);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("从下列意图中选一个最匹配用户问题的，只返回其 ref，不要解释：\n");
        for (Intent candidate : request.getCandidates()) {
            sb.append("- ").append(candidate.getRef()).append(": ").append(candidate.getLabel()).append('\n');
        }
        sb.append("用户问题：").append(request.getText());
        LlmResponse response = llm.complete(LlmRequest.systemUser("你是意图分类器", sb.toString()));
        String chosen = response == null || response.getContent() == null
                ? "" : response.getContent().trim();
        for (Intent candidate : request.getCandidates()) {
            if (chosen.contains(candidate.getRef())) {
                matches.add(new IntentMatch(candidate.getRef(), candidate.getLabel(), 1.0));
            } else {
                matches.add(new IntentMatch(candidate.getRef(), candidate.getLabel(), 0.0));
            }
        }
        return IntentResult.of(matches);
    }
}
