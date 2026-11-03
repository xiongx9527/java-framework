package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 链式检索增强：把多个 {@link RetrievalAugmentor} 按顺序串联，前一个的输出作为后一个的输入。
 *
 * <p>例如「向量检索注入」→「重排后重写上下文」→「关键词召回补足」依次作用，每环都基于上一环的结果，
 * 最终得到增强后的消息列表。若提供 {@link RetrievalListener}，会注入到链中的
 * {@link DefaultRetrievalAugmentor} 节点以透传检索事件。</p>
 */
public final class ChainedRetrievalAugmentor implements RetrievalAugmentor {

    private final List<RetrievalAugmentor> augmentors;

    private final RetrievalListener listener;

    public ChainedRetrievalAugmentor(List<RetrievalAugmentor> augmentors) {
        this(augmentors, null);
    }

    public ChainedRetrievalAugmentor(List<RetrievalAugmentor> augmentors, RetrievalListener listener) {
        if (augmentors == null || augmentors.isEmpty()) {
            throw new IllegalArgumentException("at least one augmentor is required");
        }
        this.listener = listener;
        List<RetrievalAugmentor> list = new ArrayList<RetrievalAugmentor>(augmentors.size());
        for (RetrievalAugmentor a : augmentors) {
            if (a == null) {
                continue;
            }
            if (listener != null && a instanceof DefaultRetrievalAugmentor) {
                list.add(((DefaultRetrievalAugmentor) a).withListener(listener));
            } else {
                list.add(a);
            }
        }
        if (list.isEmpty()) {
            throw new IllegalArgumentException("at least one augmentor is required");
        }
        this.augmentors = Collections.unmodifiableList(list);
    }

    public static RetrievalAugmentor of(RetrievalAugmentor... augmentors) {
        return of(null, augmentors);
    }

    public static RetrievalAugmentor of(RetrievalListener listener, RetrievalAugmentor... augmentors) {
        List<RetrievalAugmentor> list = new ArrayList<RetrievalAugmentor>();
        if (augmentors != null) {
            Collections.addAll(list, augmentors);
        }
        if (list.isEmpty()) {
            return null;
        }
        if (list.size() == 1 && listener == null) {
            return list.get(0);
        }
        return new ChainedRetrievalAugmentor(list, listener);
    }

    @Override
    public List<ChatMessage> augment(List<ChatMessage> messages, RetrievalQuery query) {
        List<ChatMessage> current = new ArrayList<ChatMessage>(messages);
        for (RetrievalAugmentor augmentor : augmentors) {
            current = augmentor.augment(current, query);
        }
        return current;
    }
}
