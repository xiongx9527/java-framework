package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentContext;
import com.xiongx9527.fw.plus.ai.base.intent.IntentMatch;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 规则引擎 / 规范 意图分类：按用户自定义的 {@link IntentRule} 列表求值。
 *
 * <p>这是“规则（正则、规则引擎等）”意图策略的可自定义形态——规则来自外部规范（代码实现
 * {@link IntentRule} 或声明式 {@link IntentRuleSpec}），本类只负责按分值降序求值并产出
 * {@link IntentResult}。可与 {@code RegexIntentClassifier}、{@code VectorIntentClassifier}、
 * {@code LlmIntentClassifier} 在 {@code HybridIntentClassifier} 中合并。</p>
 *
 * <p>要接入既有的规则引擎，只需让引擎的每次判定返回一个 {@link IntentRule} 实现即可，无需改动本类。</p>
 */
public final class RuleIntentClassifier implements IntentPort {

    private final List<IntentRule> rules;

    public RuleIntentClassifier(List<IntentRule> rules) {
        if (rules == null || rules.isEmpty()) {
            throw new IllegalArgumentException("rule intent classifier requires at least one rule");
        }
        List<IntentRule> sorted = new ArrayList<IntentRule>(rules);
        Collections.sort(sorted, new Comparator<IntentRule>() {
            @Override
            public int compare(IntentRule a, IntentRule b) {
                return Double.compare(b.getScore(), a.getScore());
            }
        });
        this.rules = Collections.unmodifiableList(sorted);
    }

    public RuleIntentClassifier(IntentRule... rules) {
        this(java.util.Arrays.asList(rules));
    }

    @Override
    public IntentResult classify(IntentRequest request) {
        List<IntentMatch> matches = new ArrayList<IntentMatch>();
        if (request == null) {
            return IntentResult.of(matches);
        }
        IntentContext ctx = new IntentContext(request.getText(), request.getAttributes());
        for (IntentRule rule : rules) {
            if (rule.test(ctx)) {
                String label = rule.getRef();
                for (Intent candidate : request.getCandidates()) {
                    if (candidate.getRef().equals(rule.getRef())) {
                        label = candidate.getLabel();
                        break;
                    }
                }
                matches.add(new IntentMatch(rule.getRef(), label, rule.getScore()));
            }
        }
        return IntentResult.of(matches);
    }
}
