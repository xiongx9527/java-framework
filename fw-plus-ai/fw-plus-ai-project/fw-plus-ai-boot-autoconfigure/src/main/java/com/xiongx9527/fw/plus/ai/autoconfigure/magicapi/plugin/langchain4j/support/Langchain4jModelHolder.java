package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 七类模型实例的持有中心：动态注册器写入/驱逐，脚本侧 Langchain4jModule 只读获取。
 * 对实现 Closeable 的实例（向量库客户端）驱逐时关闭，异常仅记日志不阻断。
 */
public class Langchain4jModelHolder {

    private static final Logger logger = LoggerFactory.getLogger(Langchain4jModelHolder.class);

    private final Map<Langchain4jModelTypes, Map<String, Object>> registry =
            new EnumMap<Langchain4jModelTypes, Map<String, Object>>(Langchain4jModelTypes.class);

    public Langchain4jModelHolder() {
        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            registry.put(type, new ConcurrentHashMap<String, Object>());
        }
    }

    public void put(Langchain4jModelTypes type, String key, Object instance) {
        map(type).put(key, instance);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Langchain4jModelTypes type, String key) {
        return (T) map(type).get(key);
    }

    public void evict(Langchain4jModelTypes type, String key) {
        Object removed = map(type).remove(key);
        if (removed instanceof Closeable) {
            try {
                ((Closeable) removed).close();
            } catch (Exception e) {
                logger.warn("关闭 LangChain4j 实例失败: type={}, key={}", type.type(), key, e);
            }
        }
    }

    public Map<String, Object> snapshot(Langchain4jModelTypes type) {
        return Collections.unmodifiableMap(map(type));
    }

    public Set<String> keys(Langchain4jModelTypes type) {
        return map(type).keySet();
    }

    public void clear() {
        for (Langchain4jModelTypes type : Langchain4jModelTypes.values()) {
            for (String key : map(type).keySet()) {
                evict(type, key);
            }
            map(type).clear();
        }
    }

    private Map<String, Object> map(Langchain4jModelTypes type) {
        return registry.get(type);
    }
}
