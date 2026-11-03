package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentMatch;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import java.util.ArrayList;
import java.util.List;

/**
 * 正则 / 关键词意图分类：把候选描述切成关键词，按命中比例打分。
 *
 * <p>零依赖、可离线、确定性强，适合规则明确的场景（如固定指令词命中技能）。</p>
 */
public final class RegexIntentClassifier implements IntentPort {

    private final double keywordMinLength;

    public RegexIntentClassifier() {
        this(2);
    }

    public RegexIntentClassifier(double keywordMinLength) {
        this.keywordMinLength = keywordMinLength;
    }

    @Override
    public IntentResult classify(IntentRequest request) {
        List<IntentMatch> matches = new ArrayList<IntentMatch>();
        if (request == null || request.getText() == null || request.getCandidates().isEmpty()) {
            return IntentResult.of(matches);
        }
        String text = request.getText().toLowerCase();
        for (Intent candidate : request.getCandidates()) {
            double score = score(text, candidate.getDescription());
            if (score > 0.0) {
                matches.add(new IntentMatch(candidate.getRef(), candidate.getLabel(), score));
            }
        }
        return IntentResult.of(matches);
    }

    private double score(String text, String description) {
        if (description == null || description.isEmpty()) {
            return 0.0;
        }
        String[] tokens = description.toLowerCase().split("[\\s,.;:!?，。；：！？、()（）\"'/]+");
        int total = 0;
        int hit = 0;
        for (String token : tokens) {
            if (token.length() < keywordMinLength) {
                continue;
            }
            total++;
            if (text.contains(token)) {
                hit++;
            }
        }
        return total == 0 ? 0.0 : (double) hit / total;
    }
}
