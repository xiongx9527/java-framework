package com.xiongx9527.fw.plus.ai.autoconfigure.store.graph;

import com.xiongx9527.fw.plus.ai.autoconfigure.store.AbstractStoreInstance;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.StoreConfig;

/**
 * 单个图库实例配置（多实例模型下的一个条目）。
 *
 * <p>继承 {@link AbstractStoreInstance} 的公共连接 / 鉴权 / 超时字段，并补充图库特有的
 * {@code database}；构建公共 {@link StoreConfig} 时把 database 一并带入。</p>
 */
public class GraphStoreInstance extends AbstractStoreInstance {

    /** 数据库名；缺省由各后端使用默认值（如 Neo4j 的 {@code neo4j}）。 */
    private String database;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    @Override
    public StoreConfig.Builder commonBuilder() {
        StoreConfig.Builder b = super.commonBuilder();
        if (database != null) {
            b.database(database);
        }
        return b;
    }
}
