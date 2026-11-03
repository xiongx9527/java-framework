package com.xiongx9527.fw.plus.ai.base.intent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 意图分类结果：候选命中的排序列表，取第一名作为最终意图。
 */
public final class IntentResult {

    private final List<IntentMatch> matches;

    private IntentResult(List<IntentMatch> matches) {
        this.matches = Collections.unmodifiableList(new ArrayList<IntentMatch>(matches));
    }

    public static IntentResult of(List<IntentMatch> matches) {
        List<IntentMatch> sorted = new ArrayList<IntentMatch>(matches);
        Collections.sort(sorted, new Comparator<IntentMatch>() {
            @Override
            public int compare(IntentMatch a, IntentMatch b) {
                return Double.compare(b.getScore(), a.getScore());
            }
        });
        return new IntentResult(sorted);
    }

    /** 全部命中（降序）。 */
    public List<IntentMatch> getMatches() {
        return matches;
    }

    /** 最高分命中；空结果返回 {@code null}。 */
    public IntentMatch getTop() {
        return matches.isEmpty() ? null : matches.get(0);
    }

    public boolean isEmpty() {
        return matches.isEmpty();
    }
}
