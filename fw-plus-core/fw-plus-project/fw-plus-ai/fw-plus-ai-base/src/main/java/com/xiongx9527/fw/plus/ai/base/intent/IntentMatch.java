package com.xiongx9527.fw.plus.ai.base.intent;

/**
 * 一个命中的意图（带分值）。
 */
public final class IntentMatch {

    private final String ref;

    private final String label;

    private final double score;

    public IntentMatch(String ref, String label, double score) {
        this.ref = ref;
        this.label = label;
        this.score = score;
    }

    public String getRef() {
        return ref;
    }

    public String getLabel() {
        return label;
    }

    public double getScore() {
        return score;
    }
}
