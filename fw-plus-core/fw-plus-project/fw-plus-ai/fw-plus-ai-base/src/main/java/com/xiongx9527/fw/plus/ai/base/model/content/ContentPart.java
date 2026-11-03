package com.xiongx9527.fw.plus.ai.base.model.content;

/**
 * 消息内容片段。实现类必须不可变。
 */
public interface ContentPart {

    ContentType type();

    /** 文本内容；非文本片段返回 {@code null}。 */
    default String text() {
        return null;
    }
}
