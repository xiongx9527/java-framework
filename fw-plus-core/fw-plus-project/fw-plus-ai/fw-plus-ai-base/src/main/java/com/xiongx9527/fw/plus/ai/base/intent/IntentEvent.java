package com.xiongx9527.fw.plus.ai.base.intent;

import com.xiongx9527.fw.plus.ai.base.event.Phase;

/**
 * 意图分类事件：分类完成后随 {@link IntentMatch} 逐个上报，带 {@link Phase} 起止。
 *
 * <ul>
 *   <li>{@code START}：分类开始（{@link #getTotal()} 为候选意图数，其余字段为空）。</li>
 *   <li>{@code END}：每条命中意图上报一次（{@link #getRank()} 为 1 基排名）。</li>
 * </ul>
 */
public final class IntentEvent {

    private final Phase phase;

    private final String ref;

    private final String label;

    private final double score;

    private final int rank;

    private final int total;

    private IntentEvent(Phase phase, String ref, String label, double score, int rank, int total) {
        this.phase = phase;
        this.ref = ref;
        this.label = label;
        this.score = score;
        this.rank = rank;
        this.total = total;
    }

    public static IntentEvent start(int candidateCount) {
        return new IntentEvent(Phase.START, null, null, 0.0, 0, candidateCount);
    }

    public static IntentEvent match(String ref, String label, double score, int rank, int total) {
        return new IntentEvent(Phase.END, ref, label, score, rank, total);
    }

    public Phase getPhase() {
        return phase;
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

    /** 1 基排名（仅 {@code END} 阶段有效）。 */
    public int getRank() {
        return rank;
    }

    /** 候选 / 命中总数（{@code START} 为候选数，{@code END} 为命中数）。 */
    public int getTotal() {
        return total;
    }
}
