package com.xiongx9527.fw.plus.ai.base.store;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 命名存储注册器：把多类后端（向量 / 图 / 自定义）按名登记，上层按名或按类型取用。
 *
 * <p>与具体存储语义解耦——registry 只持有 {@link Store} 的身份 / 可用性 / 生命周期门面；
 * 按 id 的 CRUD 走 {@link RecordStore}，向量检索走 {@code VectorStorePort}，
 * 图遍历走 {@code GraphStorePort}，按具体类型取用即可。</p>
 */
public final class StoreRegistry {

    private final Map<String, Store> stores = new LinkedHashMap<String, Store>();

    /** 登记一个命名存储；{@code name} 或 {@code store} 为 null 抛异常。 */
    public StoreRegistry register(String name, Store store) {
        if (name == null || store == null) {
            throw new IllegalArgumentException("store name and instance are required");
        }
        stores.put(name, store);
        return this;
    }

    /** 按名取存储（未登记抛 {@link IllegalStateException}）。 */
    public Store get(String name) {
        Store store = stores.get(name);
        if (store == null) {
            throw new IllegalStateException("no store registered under name: " + name);
        }
        return store;
    }

    /** 按名 + 类型取存储；类型不符抛 {@link IllegalStateException}。 */
    public <S extends Store> S get(String name, Class<S> type) {
        Store store = get(name);
        if (!type.isInstance(store)) {
            throw new IllegalStateException(
                    "store '" + name + "' is not of type " + type.getName());
        }
        return type.cast(store);
    }

    /** 取第一个匹配类型的已注册存储；无则抛 {@link IllegalStateException}。 */
    public <S extends Store> S first(Class<S> type) {
        for (Store store : stores.values()) {
            if (type.isInstance(store)) {
                return type.cast(store);
            }
        }
        throw new IllegalStateException("no registered store of type " + type.getName());
    }

    public boolean contains(String name) {
        return stores.containsKey(name);
    }

    public int size() {
        return stores.size();
    }
}
