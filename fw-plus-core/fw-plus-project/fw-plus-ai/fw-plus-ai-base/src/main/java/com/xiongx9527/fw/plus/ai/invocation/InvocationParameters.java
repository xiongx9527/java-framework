package com.xiongx9527.fw.plus.ai.invocation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 调用参数：随一次 AI Service 调用透传的任意键值。
 *
 * <p><b>作用</b>：调用方（业务代码）与实现方（工具、检索器、监听器）之间传递「不属于方法签名」的参数，
 * 例如租户 ID、数据源 ID、灰度标记。相比 ThreadLocal，它显式随调用传递，异步链路不会丢失。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.invocation.InvocationParameters}
 * （{@code asMap/get/getOrDefault/put/containsKey/from}）。</p>
 *
 * <p><b>实现约定</b>：可变对象；{@link #asMap()} 返回只读视图；跨线程共享前请复制。</p>
 *
 * @author xiongxiao
 */
public class InvocationParameters {

    private final Map<String, Object> parameters;

    /** 创建空参数集。 */
    public InvocationParameters() {
        this.parameters = new LinkedHashMap<String, Object>();
    }

    /**
     * 由 Map 复制创建。
     *
     * @param parameters 初始键值
     */
    public InvocationParameters(Map<String, Object> parameters) {
        this.parameters = parameters == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(parameters);
    }

    /**
     * 转为 Map。
     *
     * @return 只读视图
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(parameters);
    }

    /**
     * 读取参数。
     *
     * @param key 键
     * @param <T> 值类型
     * @return 值；不存在时为 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) parameters.get(key);
    }

    /**
     * 读取参数，缺失时返回默认值。
     *
     * @param key          键
     * @param defaultValue 默认值
     * @param <T>          值类型
     * @return 值或默认值
     */
    public <T> T getOrDefault(String key, T defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        @SuppressWarnings("unchecked")
        T typed = (T) value;
        return typed;
    }

    /**
     * 写入参数。
     *
     * @param key   键
     * @param value 值
     * @param <T>   值类型
     */
    public <T> void put(String key, T value) {
        parameters.put(key, value);
    }

    /**
     * 是否包含键。
     *
     * @param key 键
     * @return 包含返回 {@code true}
     */
    public boolean containsKey(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public String toString() {
        return "InvocationParameters" + parameters;
    }

    /**
     * 由单个键值创建。
     *
     * @param key   键
     * @param value 值
     * @return 参数集
     */
    public static InvocationParameters from(String key, Object value) {
        InvocationParameters parameters = new InvocationParameters();
        parameters.put(key, value);
        return parameters;
    }

    /**
     * 由 Map 创建。
     *
     * @param parameters 键值
     * @return 参数集
     */
    public static InvocationParameters from(Map<String, Object> parameters) {
        return new InvocationParameters(parameters);
    }
}
