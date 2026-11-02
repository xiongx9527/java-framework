package com.xiongx9527.fw.plus.ai.message;

/**
 * 聊天消息内容接口。
 *
 * <p><b>作用</b>：表示一条聊天消息中的一种内容块（文本 / 图片 / 音频 / 视频 / PDF 等），
 * 支持多模态消息由多个内容块组成。</p>
 *
 * <p><b>出处</b>：镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.data.message.Content}
 * （{@code type()}）；实现类 {@link TextContent} / {@link ImageContent} 对应
 * {@code TextContent} / {@code ImageContent}。Spring AI 等价 {@code org.springframework.ai.content.Media} /
 * {@code TextContent} 等。</p>
 *
 * <p><b>实现约定</b>：实现类应为不可变对象；{@link #type()} 用于下游按类型分派处理。</p>
 */
public interface Content {

    /**
     * 返回内容类型。
     *
     * @return 内容类型枚举，非 {@code null}
     */
    ContentType type();
}
