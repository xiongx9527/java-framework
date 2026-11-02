package com.xiongx9527.fw.plus.ai.message;

import java.util.List;

/**
 * 消息 JSON 编解码端口：在消息对象与 JSON 文本之间互转。
 *
 * <p><b>作用</b>：对话记忆落库、消息跨进程传递、审计留痕都需要稳定的 JSON 形态。
 * 序列化细节（字段名、多态标记）由实现决定，端口只约定四个方向的方法。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.message.ChatMessageJsonCodec}
 * （官方另有 Jackson 实现 {@code JacksonChatMessageJsonCodec}，属适配器职责，不进端口层）。</p>
 *
 * <p><b>实现约定</b>：解析失败抛 {@link com.xiongx9527.fw.plus.ai.exception.AiException}；
 * 实现必须无状态、线程安全。</p>
 *
 * @author xiongxiao
 */
public interface ChatMessageJsonCodec {

    /**
     * 解析单条消息。
     *
     * @param json JSON 文本
     * @return 消息
     */
    ChatMessage messageFromJson(String json);

    /**
     * 解析消息列表。
     *
     * @param json JSON 文本
     * @return 消息列表
     */
    List<ChatMessage> messagesFromJson(String json);

    /**
     * 序列化单条消息。
     *
     * @param message 消息
     * @return JSON 文本
     */
    String messageToJson(ChatMessage message);

    /**
     * 序列化消息列表。
     *
     * @param messages 消息列表
     * @return JSON 文本
     */
    String messagesToJson(List<ChatMessage> messages);
}
