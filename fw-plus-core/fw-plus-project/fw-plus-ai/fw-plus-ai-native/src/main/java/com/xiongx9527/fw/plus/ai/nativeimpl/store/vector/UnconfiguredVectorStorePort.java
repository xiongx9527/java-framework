package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import java.util.Map;

/**
 * 未配置的向量库占位实现：应用未设置 {@code fw.plus.ai.vector-store.base-url} 时由自动装配注入。
 *
 * <p>{@link #configured()} 返回 {@code false} —— 这是「未配置」与「不可达」的明确区分；所有读写方法
 * 与 {@link #health()} 一律抛 {@link IllegalStateException}（带明确引导信息），保证其它功能不受影响、
 * 应用照常启动。需要向量库时，补上 base-url 即切换为真实后端，无需改代码。</p>
 */
public final class UnconfiguredVectorStorePort implements VectorStorePort {

    private static final String MSG =
            "vector store is not configured; set fw.plus.ai.vector-store.base-url to enable it";

    @Override
    public boolean configured() {
        return false;
    }

    @Override
    public boolean health() {
        throw new IllegalStateException(MSG);
    }

    @Override
    public String name() {
        return "unconfigured";
    }

    @Override
    public StoreType type() {
        return StoreType.VECTOR;
    }

    @Override
    public String defaultCollection() {
        return "default";
    }

    @Override
    public void ensureCollection(String collection, int dimension) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void dropCollection(String collection) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void delete(String collection, String id) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        throw new IllegalStateException(MSG);
    }
}
