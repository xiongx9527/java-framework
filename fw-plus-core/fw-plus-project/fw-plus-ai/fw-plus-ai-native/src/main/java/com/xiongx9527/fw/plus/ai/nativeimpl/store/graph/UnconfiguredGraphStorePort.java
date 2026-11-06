package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.RecordStore;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphEdge;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphNode;
import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 未配置的图库占位实现：应用未设置 {@code fw.plus.ai.graph-store.base-url} 时由自动装配注入。
 *
 * <p>{@link #configured()} 返回 {@code false} —— 这是「未配置」与「不可达」的明确区分；所有读写方法
 * 一律抛 {@link IllegalStateException}（带明确引导信息），保证其它功能不受影响、应用照常启动。
 * 需要图库时，补上 base-url 即切换为真实后端，无需改代码。</p>
 */
public final class UnconfiguredGraphStorePort implements GraphStorePort {

    private static final String MSG =
            "graph store is not configured; set fw.plus.ai.graph-store.base-url to enable it";

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
        return StoreType.GRAPH;
    }

    @Override
    public void clear() {
        throw new IllegalStateException(MSG);
    }

    @Override
    public long count() {
        throw new IllegalStateException(MSG);
    }

    @Override
    public List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public List<GraphNode> nodes(Collection<String> ids) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public List<GraphNode> searchNodes(String text, int topK) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void save(StoreRecord record) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public StoreRecord get(String id) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public List<StoreRecord> getAll(Collection<String> ids) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public boolean delete(String id) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public List<StoreRecord> search(StoreQuery query) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void saveAll(Collection<StoreRecord> records) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public void update(StoreRecord record) {
        throw new IllegalStateException(MSG);
    }

    @Override
    public long deleteAll(Collection<String> ids) {
        throw new IllegalStateException(MSG);
    }
}
