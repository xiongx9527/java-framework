package com.xiongx9527.fw.plus.ai.rag;

import com.xiongx9527.fw.plus.ai.message.ChatMessage;
import com.xiongx9527.fw.plus.ai.rag.content.Content;
import com.xiongx9527.fw.plus.ai.retriever.ContentRetriever;
import com.xiongx9527.fw.plus.ai.retriever.Query;

import java.util.Collections;
import java.util.List;

/**
 * 默认检索增强器：检索内容后原样回传，注入由 {@code ContentInjector} 另行完成。
 *
 * <p><b>作用</b>：最小可用的 {@link RetrievalAugmentor}——只负责「按消息文本检索」，
 * 不做查询改写、路由与聚合；需要这些能力时组合对应组件。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.DefaultRetrievalAugmentor}
 * 的精简形态（官方完整版包含 transformer / router / aggregator / injector 四段流水线，
 * 本类只保留 retriever 一段，其余由组合使用）。</p>
 *
 * @author xiongxiao
 */
public class DefaultRetrievalAugmentor implements RetrievalAugmentor {

    private final ContentRetriever contentRetriever;

    private DefaultRetrievalAugmentor(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    @Override
    public AugmentationResult augment(AugmentationRequest request) {
        ChatMessage chatMessage = request == null ? null : request.chatMessage();
        if (contentRetriever == null || chatMessage == null) {
            return new AugmentationResult(chatMessage, Collections.<Content>emptyList());
        }
        List<Content> contents = contentRetriever.retrieve(new Query(chatMessage.text(), request.metadata()));
        return new AugmentationResult(chatMessage, contents);
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link DefaultRetrievalAugmentor} 构建器。 */
    public static final class Builder {
        private ContentRetriever contentRetriever;

        public Builder contentRetriever(ContentRetriever contentRetriever) {
            this.contentRetriever = contentRetriever;
            return this;
        }

        public DefaultRetrievalAugmentor build() {
            return new DefaultRetrievalAugmentor(contentRetriever);
        }
    }
}
