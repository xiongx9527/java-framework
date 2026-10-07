package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

/**
 * ChatMemoryStore 租户解析器：为存储层提供当前租户 ID（{@code tenant_id} 维度）。
 *
 * <p>自动配置默认注册返回配置项 {@code fw.plus.ai.langchain4j.chat-memory.default-tenant-id}
 * 的实现；使用方提供自己的 Bean（如从登录上下文/请求头解析）即自动覆盖默认实现。</p>
 */
public interface ChatMemoryTenantResolver {

    /**
     * 解析当前租户 ID，返回 {@code null} 时存储层使用默认租户。
     */
    String resolveTenantId();
}
