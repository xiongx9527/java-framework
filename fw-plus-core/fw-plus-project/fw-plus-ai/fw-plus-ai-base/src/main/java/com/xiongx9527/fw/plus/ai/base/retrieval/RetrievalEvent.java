package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.event.Phase;
import java.util.Collections;
import java.util.List;

/**
 * 检索事件：带 {@link Phase} 的过程事件。
 *
 * <ul>
 *   <li>{@code START}：检索发起（携带 {@link #getQuery()}）。</li>
 *   <li>{@code END}：检索完成（携带 {@link #getHits()} 与注入上下文 {@link #getContent()}）。</li>
 * </ul>
 * 与 {@link RetrievalListener} 对齐，便于桥接到流式回调。</p>
 */
public final class RetrievalEvent {

    private final Phase phase;

    private final RetrievalQuery query;

    private final List<? extends Retrieved<?>> hits;

    private final String content;

    private RetrievalEvent(Phase phase, RetrievalQuery query, List<? extends Retrieved<?>> hits, String content) {
        this.phase = phase;
        this.query = query;
        this.hits = hits == null ? Collections.<Retrieved<?>>emptyList()
                : Collections.unmodifiableList(hits);
        this.content = content;
    }

    public static RetrievalEvent start(RetrievalQuery query) {
        return new RetrievalEvent(Phase.START, query, null, null);
    }

    public static RetrievalEvent end(RetrievalQuery query, List<? extends Retrieved<?>> hits, String content) {
        return new RetrievalEvent(Phase.END, query, hits, content);
    }

    public Phase getPhase() {
        return phase;
    }

    public RetrievalQuery getQuery() {
        return query;
    }

    public List<? extends Retrieved<?>> getHits() {
        return hits;
    }

    public int getHitCount() {
        return hits.size();
    }

    /** 注入到对话的上下文内容（{@code END} 阶段才有）。 */
    public String getContent() {
        return content;
    }
}
