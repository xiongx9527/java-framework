package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageJsonCodec;
import dev.langchain4j.data.message.GsonChatMessageJsonCodec;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 逐条消息存储型 {@link ChatMemoryStore} 的公共基类。
 *
 * <p>存储模型（各介质统一）：</p>
 * <ul>
 *   <li><b>维度</b>：{@code tenant_id}（租户，经 {@link ChatMemoryTenantResolver} 解析）、
 *       {@code memory_id}（完整原始值，约定 {@code {bizType}:{conversationId}}）、
 *       {@code biz_type}（memoryId 冒号前缀解析出的业务类型，冗余存储便于按业务查询）；</li>
 *   <li><b>消息</b>：每条消息一行/一篇文档，携带 {@code message_type}
 *       （{@code ChatMessageType} 名称：SYSTEM / USER / AI / TOOL_EXECUTION_RESULT）、
 *       {@code message_seq}（0 起排序号）与单条消息 JSON；</li>
 *   <li><b>契约</b>：{@code updateMessages} 语义为全量覆盖——按 {@code message_seq} 原位覆盖
 *       （upsert）已有序号并裁剪超出部分，<b>不做先删后插</b>，避免并发读到空窗；
 *       {@code create_time} 首次写入后保持不变，{@code update_time} 随每次覆盖刷新。</li>
 * </ul>
 *
 * <p>子类只需实现 {@code readMessages / writeMessages / removeMessages} 三个存储原语，
 * 不必关心租户解析、业务类型解析与消息编解码。</p>
 */
public abstract class AbstractJsonChatMemoryStore implements ChatMemoryStore {

    private static final ChatMessageJsonCodec CODEC = new GsonChatMessageJsonCodec();

    private final ChatMemoryTenantResolver tenantResolver;

    private final String defaultBizType;

    protected AbstractJsonChatMemoryStore(ChatMemoryTenantResolver tenantResolver, String defaultBizType) {
        this.tenantResolver = tenantResolver;
        this.defaultBizType = defaultBizType == null || defaultBizType.trim().isEmpty()
                ? ChatMemoryIds.DEFAULT_BIZ_TYPE : defaultBizType;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = String.valueOf(memoryId);
        List<StoredMessage> stored = readMessages(resolveTenantId(), id, resolveBizType(id));
        if (stored == null || stored.isEmpty()) {
            return Collections.emptyList();
        }
        List<ChatMessage> result = new ArrayList<>(stored.size());
        for (StoredMessage row : stored) {
            result.add(CODEC.messagesFromJson(row.getJson()).get(0));
        }
        return result;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);
        List<StoredMessage> stored = new ArrayList<>(messages == null ? 0 : messages.size());
        if (messages != null) {
            for (ChatMessage message : messages) {
                // 单条消息以「单元素 JSON 数组」形式存储，便于统一走 codec
                stored.add(new StoredMessage(message.type().name(),
                        CODEC.messagesToJson(Collections.singletonList(message))));
            }
        }
        writeMessages(resolveTenantId(), id, resolveBizType(id), stored);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        removeMessages(resolveTenantId(), String.valueOf(memoryId));
    }

    /** 解析当前租户 ID；解析器缺失或返回 {@code null} 时使用默认租户。 */
    protected String resolveTenantId() {
        String tenantId = tenantResolver == null ? null : tenantResolver.resolveTenantId();
        return tenantId == null || tenantId.trim().isEmpty()
                ? ChatMemoryStoreDefaults.DEFAULT_TENANT_ID : tenantId;
    }

    private String resolveBizType(String memoryId) {
        return ChatMemoryIds.parseBizType(memoryId, defaultBizType);
    }

    /** 按序号升序读取 {@code tenant_id + memory_id} 下的消息；不存在时返回空列表。 */
    protected abstract List<StoredMessage> readMessages(String tenantId, String memoryId, String bizType);

    /** 覆盖 {@code tenant_id + memory_id} 下的消息：原位 upsert 已有序号、裁剪多余序号（不做先删后插）。 */
    protected abstract void writeMessages(String tenantId, String memoryId, String bizType,
            List<StoredMessage> messages);

    /** 删除 {@code tenant_id + memory_id} 下的全部消息。 */
    protected abstract void removeMessages(String tenantId, String memoryId);

    /** 单条存储消息：{@code ChatMessageType} 名称 + 单元素 JSON 数组形式的消息内容。 */
    protected static final class StoredMessage {

        private final String messageType;

        private final String json;

        public StoredMessage(String messageType, String json) {
            this.messageType = messageType;
            this.json = json;
        }

        public String getMessageType() {
            return messageType;
        }

        public String getJson() {
            return json;
        }
    }
}
