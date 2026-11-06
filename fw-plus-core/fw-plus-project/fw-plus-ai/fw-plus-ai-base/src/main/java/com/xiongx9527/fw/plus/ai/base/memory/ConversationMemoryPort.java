package com.xiongx9527.fw.plus.ai.base.memory;

import java.util.Collections;
import java.util.List;

/**
 * 对话记忆后端端口：mem0 风格的语义记忆存储（写入对话、按查询检索事实）。
 *
 * <p>与 {@link ChatMemoryStore}（窗口式「取/覆盖消息」）不同，本端口面向<b>语义记忆</b>：
 * 后端会用 LLM 把对话拆成事实、向量化后存储，并提供自然语言检索。多个提供商
 * （mem0 自托管服务 / 本地向量库 / 进程内存）实现同一端口，由
 * {@code nativeimpl.memory.ConversationMemoryPortResolver} 按协议或厂商路由，业务侧零改动切换。</p>
 *
 * <p>失败语义：网络不可达、鉴权失败、后端 4xx/5xx 统一抛 {@link ConversationMemoryException}，
 * 由上层决定重试或降级；{@code add} 返回的是本次<b>实际写入/变更</b>的记忆列表（mem0 的
 * {@code results}），而非回显入参。</p>
 */
public interface ConversationMemoryPort {

    /** 写入一段对话，由后端抽取/存储为记忆；返回本次新增或更新的记忆。 */
    List<MemoryRecord> add(ConversationMemoryAddRequest request);

    /** 按自然语言查询检索相关记忆；无匹配时返回空列表（不返回 {@code null}）。 */
    List<MemoryRecord> search(ConversationMemorySearchRequest request);

    /** 列出某作用域下的全部记忆（按创建时间正序）。 */
    List<MemoryRecord> list(ConversationMemoryScope scope);

    /** 删除单条记忆。 */
    void delete(String memoryId);

    /** 删除某作用域下的全部记忆。 */
    void deleteAll(ConversationMemoryScope scope);

    /** 端口级监听器；业务侧可覆写以接入日志 / 计费 / 追踪。 */
    default List<ConversationMemoryListener> listeners() {
        return Collections.emptyList();
    }
}
