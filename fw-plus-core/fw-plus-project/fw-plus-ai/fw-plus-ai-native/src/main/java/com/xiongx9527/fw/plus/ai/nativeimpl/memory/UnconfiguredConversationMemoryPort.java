package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import java.util.List;

/**
 * 未配置占位端口：对话记忆后端未配置任何提供商时暴露，保证应用缺省也能启动；
 * 任何读写调用都抛 {@link ConversationMemoryException}（区别于「已配置但不可达」）。
 *
 * <p>与 {@code UnconfiguredVectorStorePort} / {@code UnconfiguredGraphStorePort} 同构，
 * 由 {@link ConversationMemoryPortResolver} 在未配置时返回，业务侧无需判空。</p>
 */
public class UnconfiguredConversationMemoryPort implements ConversationMemoryPort {

    private static final String MSG =
            "conversation memory is not configured; set fw.plus.ai.memory.provider/base-url or "
                    + "wire an EmbeddingPort + VectorStorePort for the VECTOR provider";

    @Override
    public List<MemoryRecord> add(ConversationMemoryAddRequest request) {
        throw new ConversationMemoryException(MSG);
    }

    @Override
    public List<MemoryRecord> search(ConversationMemorySearchRequest request) {
        throw new ConversationMemoryException(MSG);
    }

    @Override
    public List<MemoryRecord> list(ConversationMemoryScope scope) {
        throw new ConversationMemoryException(MSG);
    }

    @Override
    public void delete(String memoryId) {
        throw new ConversationMemoryException(MSG);
    }

    @Override
    public void deleteAll(ConversationMemoryScope scope) {
        throw new ConversationMemoryException(MSG);
    }
}
