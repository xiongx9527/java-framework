package com.xiongx9527.fw.plus.ai.invocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 由框架托管的资源（连接、客户端、会话等）的线程级注册表。
 *
 * <p><b>作用</b>：某些资源需要「在调用线程内全局可查、但不进入方法签名」（如当前请求绑定的
 * 数据源客户端）。本接口用 {@code ThreadLocal} 提供注册 / 查询 / 清理，供框架组件按类型取用。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.invocation.LangChain4jManaged}。</p>
 *
 * <p><b>实现约定</b>：使用方必须在 {@code finally} 中调用 {@link #removeCurrent()}，
 * 否则线程复用（线程池）会导致资源串味。</p>
 *
 * @author xiongxiao
 */
public interface LangChain4jManaged {

    /** 当前线程托管资源表。 */
    ThreadLocal<Map<Class<? extends LangChain4jManaged>, LangChain4jManaged>> CURRENT =
            new ThreadLocal<Map<Class<? extends LangChain4jManaged>, LangChain4jManaged>>();

    /**
     * 设置当前线程的托管资源。
     *
     * @param resources 资源表
     */
    static void setCurrent(Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> resources) {
        CURRENT.set(resources == null
                ? Collections.<Class<? extends LangChain4jManaged>, LangChain4jManaged>emptyMap()
                : new LinkedHashMap<Class<? extends LangChain4jManaged>, LangChain4jManaged>(resources));
    }

    /**
     * 读取当前线程的托管资源。
     *
     * @return 资源表，永不为 {@code null}
     */
    static Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> current() {
        Map<Class<? extends LangChain4jManaged>, LangChain4jManaged> resources = CURRENT.get();
        return resources == null
                ? Collections.<Class<? extends LangChain4jManaged>, LangChain4jManaged>emptyMap()
                : resources;
    }

    /**
     * 按类型读取当前线程的托管资源。
     *
     * @param type 资源类型
     * @param <T>  资源类型
     * @return 资源；不存在时为 {@code null}
     */
    static <T extends LangChain4jManaged> T current(Class<T> type) {
        return type == null ? null : type.cast(current().get(type));
    }

    /** 清理当前线程的托管资源。 */
    static void removeCurrent() {
        CURRENT.remove();
    }
}
