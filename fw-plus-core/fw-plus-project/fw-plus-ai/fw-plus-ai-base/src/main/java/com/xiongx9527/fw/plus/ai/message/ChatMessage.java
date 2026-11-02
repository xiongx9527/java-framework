package com.xiongx9527.fw.plus.ai.message;

import java.util.Collections;
import java.util.List;

/**
 * 聊天消息接口。
 *
 * <p><b>作用</b>：对话上下文的最小单元，供 {@code ChatModel}、{@code AiServices}、{@code ChatMemory}
 * 等统一消费。四种实现：{@link SystemMessage}、{@link UserMessage}、{@link AiMessage}、
 * {@link ToolExecutionResultMessage}。</p>
 *
 * <p><b>出处</b>：镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.data.message.ChatMessage}
 * （{@code type()} / {@code contents()}）；Spring AI 等价 {@code org.springframework.ai.chat.messages.Message}。</p>
 *
 * <p><b>实现约定</b>：实现类应为不可变对象；{@link #text()} 是 {@link #contents()} 中文本部分的便捷聚合。</p>
 */
public interface ChatMessage {

    /**
     * 返回消息类型。
     *
     * @return 消息类型枚举，非 {@code null}
     */
    ChatMessageType type();

    /**
     * 返回消息的内容块列表（可能包含文本、图片等多模态内容）。
     *
     * @return 内容块列表；默认空列表，不返回 {@code null}
     */
    default List<Content> contents() { return Collections.emptyList(); }

    /**
     * 返回消息的纯文本表示（多模态时取文本块拼接的便捷值）。
     *
     * @return 文本内容；无文本时返回空串，不返回 {@code null}
     */
    default String text() { return ""; }
}
