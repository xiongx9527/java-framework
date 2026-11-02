package com.xiongx9527.fw.plus.ai.model.embedding.request;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.message.Content;
import com.xiongx9527.fw.plus.ai.message.ContentType;
import com.xiongx9527.fw.plus.ai.message.TextContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 单条嵌入输入。
 *
 * <p><b>作用</b>：把「纯文本」与「多模态内容块」统一成一种输入形态，
 * 使多模态嵌入模型（图文混嵌）与纯文本模型共用同一个请求结构。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.request.EmbeddingInput}。</p>
 *
 * <p><b>实现约定</b>：内容块列表不可变；{@link #text()} 只在输入为单个文本块时返回文本，
 * 多模态输入返回 {@code null}，调用方需自行判断。</p>
 *
 * @author xiongxiao
 */
public class EmbeddingInput {

    private final List<Content> contents;

    /**
     * 构造。
     *
     * @param contents 内容块列表，不能为 {@code null}
     */
    protected EmbeddingInput(List<Content> contents) {
        if (contents == null) {
            throw new IllegalArgumentException("contents must not be null");
        }
        this.contents = Collections.unmodifiableList(new ArrayList<Content>(contents));
    }

    /**
     * 内容块列表。
     *
     * @return 只读列表，非 {@code null}
     */
    public List<Content> contents() {
        return contents;
    }

    /**
     * 内容块类型集合。
     *
     * @return 类型集合
     */
    public Set<ContentType> contentTypes() {
        Set<ContentType> types = EnumSet.noneOf(ContentType.class);
        for (Content content : contents) {
            types.add(content.type());
        }
        return types;
    }

    /**
     * 纯文本形式的输入。
     *
     * @return 文本；输入不是单一文本块时为 {@code null}
     */
    public String text() {
        if (contents.size() != 1 || !(contents.get(0) instanceof TextContent)) {
            return null;
        }
        return ((TextContent) contents.get(0)).text();
    }

    /**
     * 由文本创建输入。
     *
     * @param text 文本
     * @return 输入对象
     */
    public static EmbeddingInput from(String text) {
        return new EmbeddingInput(Collections.<Content>singletonList(TextContent.from(text)));
    }

    /**
     * 由文本片段创建输入。
     *
     * @param textSegment 文本片段
     * @return 输入对象
     */
    public static EmbeddingInput from(TextSegment textSegment) {
        return from(textSegment.text());
    }

    /**
     * 由内容块创建输入。
     *
     * @param contents 内容块
     * @return 输入对象
     */
    public static EmbeddingInput from(Content... contents) {
        return from(Arrays.asList(contents));
    }

    /**
     * 由内容块列表创建输入。
     *
     * @param contents 内容块列表
     * @return 输入对象
     */
    public static EmbeddingInput from(List<Content> contents) {
        return new EmbeddingInput(contents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddingInput)) {
            return false;
        }
        EmbeddingInput that = (EmbeddingInput) o;
        return Objects.equals(contents, that.contents);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(contents);
    }

    @Override
    public String toString() {
        return "EmbeddingInput{contents=" + contents + "}";
    }
}
