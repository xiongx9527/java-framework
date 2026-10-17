package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store;

import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.AgenticScopeStore;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 {@link AgenticScopeStore} 实现（缺省后端）。
 *
 * <p>直接持有 scope 引用、不做序列化：读写零转换开销，与「不装存储」的行为差异最小
 * （仅 scope 从 {@code REGISTERED} 变为 {@code PERSISTENT}，registry 清理后仍可从本 store 回读）。
 * 进程重启即丢失；跨进程 / 跨重启持久化请用 {@link MongoAgenticScopeStore}。</p>
 */
public class InMemoryAgenticScopeStore implements AgenticScopeStore {

    private final Map<AgenticScopeKey, DefaultAgenticScope> scopes = new ConcurrentHashMap<>();

    @Override
    public boolean save(AgenticScopeKey key, DefaultAgenticScope agenticScope) {
        scopes.put(key, agenticScope);
        return true;
    }

    @Override
    public Optional<DefaultAgenticScope> load(AgenticScopeKey key) {
        return Optional.ofNullable(scopes.get(key));
    }

    @Override
    public boolean delete(AgenticScopeKey key) {
        return scopes.remove(key) != null;
    }

    @Override
    public Set<AgenticScopeKey> getAllKeys() {
        return Set.copyOf(scopes.keySet());
    }

    /** 当前持有的 scope 数量（运维 / 测试观察用）。 */
    public int size() {
        return scopes.size();
    }

    /** 清空所有 scope。 */
    public void clear() {
        scopes.clear();
    }
}
