package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 单类型 LangChain4j 实例持有者骨架：一个数据源类型一个持有者，按 {@code key} 持有与驱逐实例，
 * 驱逐时关闭实现 {@link Closeable} 的实例（异常仅记日志，不阻断其它 key），应用关闭时统一清空。
 *
 * @param <T> 该数据源类型对应的实例类型
 */
public abstract class AbstractMagicDynamicLangchain4jDataSource<T> implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMagicDynamicLangchain4jDataSource.class);

    private final Langchain4jModelTypes type;

    private final Map<String, T> instances = new ConcurrentHashMap<>();

    protected AbstractMagicDynamicLangchain4jDataSource(Langchain4jModelTypes type) {
        this.type = type;
    }

    /** 该持有者对应的资源类型（同时是 datasource type 与 storage folder）。 */
    public final Langchain4jModelTypes type() {
        return type;
    }

    /** 存入实例；同 key 覆盖时不关闭旧实例，调用方应先 {@link #evict(String)}。 */
    public void put(String key, T instance) {
        if (key == null) {
            return;
        }
        instances.put(key, instance);
    }

    /** 按 key 取实例，不存在时返回 {@code null}。 */
    public T get(String key) {
        return key == null ? null : instances.get(key);
    }

    /** 驱逐实例：可关闭的实例会被关闭，关闭异常只记日志。 */
    public void evict(String key) {
        if (key == null) {
            return;
        }
        close(instances.remove(key), key);
    }

    /** 当前已配置的 key 集合（不可修改视图）。 */
    public Set<String> keys() {
        return Collections.unmodifiableSet(instances.keySet());
    }

    /** 当前实例快照（不可修改视图）。 */
    public Map<String, T> snapshot() {
        return Collections.unmodifiableMap(instances);
    }

    @Override
    public void destroy() {
        for (String key : new ArrayList<>(instances.keySet())) {
            evict(key);
        }
    }

    private void close(T instance, String key) {
        if (!(instance instanceof Closeable)) {
            return;
        }
        try {
            ((Closeable) instance).close();
        } catch (Exception e) {
            logger.warn("关闭 LangChain4j 实例失败: type={}, key={}", type.type(), key, e);
        }
    }
}
