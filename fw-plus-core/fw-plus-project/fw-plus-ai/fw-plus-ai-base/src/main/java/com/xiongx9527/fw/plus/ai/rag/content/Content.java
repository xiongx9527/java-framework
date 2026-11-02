package com.xiongx9527.fw.plus.ai.rag.content;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;

import java.util.Map;

/**
 * RAG 内容：检索得到的一段文本 + 内容级元数据。
 *
 * <p><b>作用</b>：检索器（ContentRetriever）的输出、聚合器（ContentAggregator）的输入输出、
 * 注入器（ContentInjector）的输入，是 RAG 链路贯穿始终的数据单元。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.rag.content.Content}：
 * {@code textSegment()} + {@code metadata(): Map<ContentMetadata, Object>} + 三个 {@code from} 静态工厂；
 * 默认实现见 {@link DefaultContent}。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #textSegment()} 与 {@link #metadata()} 均不得返回 {@code null}。</p>
 *
 * @author xiongxiao
 */
public interface Content {

    /**
     * 内容对应的文本片段（文本 + 文档元数据）。
     *
     * @return 文本片段，永不为 {@code null}
     */
    TextSegment textSegment();

    /**
     * 内容级元数据（得分、重排分、向量 ID 等），键见 {@link ContentMetadata}。
     *
     * @return 只读元数据，永不为 {@code null}
     */
    Map<ContentMetadata, Object> metadata();

    /**
     * 由纯文本创建内容（空元数据）。
     *
     * @param text 文本
     * @return 内容
     */
    static Content from(String text) {
        return new DefaultContent(text);
    }

    /**
     * 由文本片段创建内容（空元数据）。
     *
     * @param textSegment 文本片段
     * @return 内容
     */
    static Content from(TextSegment textSegment) {
        return new DefaultContent(textSegment);
    }

    /**
     * 由文本片段与内容元数据创建。
     *
     * @param textSegment 文本片段
     * @param metadata    内容元数据
     * @return 内容
     */
    static Content from(TextSegment textSegment, Map<ContentMetadata, Object> metadata) {
        return new DefaultContent(textSegment, metadata);
    }
}
