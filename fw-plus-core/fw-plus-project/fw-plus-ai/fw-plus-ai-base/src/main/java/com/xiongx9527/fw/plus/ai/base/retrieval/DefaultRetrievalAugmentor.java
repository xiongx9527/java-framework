package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.ChatRole;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认检索增强：检索文档 → 拼成一段上下文 → 注入 system 消息（已有 system 则追加）。
 *
 * <p>轻量但自洽，覆盖「无检索命中则原样返回」的退化情形；更复杂的融合 / 重排应由 support 提供。
 * 若提供 {@link RetrievalListener}，检索事件（查询 / 命中 / 上下文注入）会回调它。</p>
 */
public final class DefaultRetrievalAugmentor implements RetrievalAugmentor {

    private final Retriever<Document> retriever;

    private final int maxDocs;

    private final RetrievalListener listener;

    public DefaultRetrievalAugmentor(Retriever<Document> retriever) {
        this(retriever, 5, null);
    }

    public DefaultRetrievalAugmentor(Retriever<Document> retriever, int maxDocs) {
        this(retriever, maxDocs, null);
    }

    public DefaultRetrievalAugmentor(Retriever<Document> retriever, int maxDocs, RetrievalListener listener) {
        if (retriever == null) {
            throw new IllegalArgumentException("retriever is required");
        }
        this.retriever = retriever;
        this.maxDocs = maxDocs <= 0 ? 5 : maxDocs;
        this.listener = listener;
    }

    /** 返回带新监听器的副本（原实例不变）。 */
    public DefaultRetrievalAugmentor withListener(RetrievalListener value) {
        return new DefaultRetrievalAugmentor(retriever, maxDocs, value);
    }

    @Override
    public List<ChatMessage> augment(List<ChatMessage> messages, RetrievalQuery query) {
        if (listener != null) {
            listener.onQuery(query);
        }
        List<Retrieved<Document>> hits = retriever.retrieve(query);
        if (hits == null || hits.isEmpty()) {
            return new ArrayList<ChatMessage>(messages);
        }
        if (listener != null) {
            listener.onHits(hits);
        }
        StringBuilder ctx = new StringBuilder();
        int limit = Math.min(hits.size(), maxDocs);
        for (int i = 0; i < limit; i++) {
            Document doc = hits.get(i).getItem();
            if (doc == null) {
                continue;
            }
            ctx.append("[").append(i + 1).append("] ").append(doc.getContent()).append('\n');
        }
        if (ctx.length() == 0) {
            return new ArrayList<ChatMessage>(messages);
        }
        if (listener != null) {
            listener.onContext(ctx.toString());
        }
        return inject(messages, ctx.toString());
    }

    private List<ChatMessage> inject(List<ChatMessage> messages, String context) {
        List<ChatMessage> out = new ArrayList<ChatMessage>(messages);
        String block = "Use the following retrieved context to answer the question. "
                + "If the context does not contain the answer, say you do not know.\n\n" + context;
        if (!out.isEmpty() && out.get(0).getRole() == ChatRole.SYSTEM) {
            ChatMessage sys = out.get(0);
            out.set(0, ChatMessage.system((sys.getContent() == null ? "" : sys.getContent() + "\n\n") + block));
        } else {
            out.add(0, ChatMessage.system(block));
        }
        return out;
    }
}
