package com.xiongx9527.fw.plus.ai.data.document;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文档元数据：字符串键 → 标量值，可链式写入。
 *
 * <p><b>作用</b>：文档 / 文本片段携带的附属信息（文件名、绝对路径、来源 URL、页码、行号等）。
 * 检索、过滤、引用溯源都依赖它。值类型限定为可序列化的标量（String / UUID / 数值），
 * 避免把任意对象塞进元数据导致跨进程传递失败。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.document.Metadata}，
 * 方法集完全一致（{@code getString/getUUID/getInteger/getLong/getFloat/getDouble/containsKey/put 重载/
 * putAll/remove/copy/toMap/from/metadata/merge}）。Spring AI 等价 {@code org.springframework.ai.document.Document#getMetadata()}。</p>
 *
 * <p><b>实现约定</b>：可变对象（{@code put} 返回自身以便链式调用）；{@link #toMap()} 返回只读视图；
 * 跨线程共享前应 {@link #copy()}。</p>
 *
 * @author xiongxiao
 */
public class Metadata {

    private final Map<String, Object> metadata;

    /** 创建空元数据。 */
    public Metadata() {
        this.metadata = new LinkedHashMap<String, Object>();
    }

    /**
     * 从已有 Map 复制创建。
     *
     * @param metadata 初始键值，为 {@code null} 时视为空
     */
    public Metadata(Map<String, ?> metadata) {
        this.metadata = metadata == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(metadata);
    }

    /**
     * 读取字符串值。
     *
     * @param key 键
     * @return 值的字符串形式；不存在时为 {@code null}
     */
    public String getString(String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    /**
     * 读取 UUID 值。
     *
     * @param key 键
     * @return UUID；不存在时为 {@code null}
     */
    public UUID getUUID(String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof UUID ? (UUID) value : UUID.fromString(value.toString());
    }

    /**
     * 读取整数值。
     *
     * @param key 键
     * @return 整数；不存在时为 {@code null}
     */
    public Integer getInteger(String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(value.toString());
    }

    /**
     * 读取长整数值。
     *
     * @param key 键
     * @return 长整数；不存在时为 {@code null}
     */
    public Long getLong(String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(value.toString());
    }

    /**
     * 读取浮点值。
     *
     * @param key 键
     * @return 浮点数；不存在时为 {@code null}
     */
    public Float getFloat(String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).floatValue() : Float.valueOf(value.toString());
    }

    /**
     * 读取双精度值。
     *
     * @param key 键
     * @return 双精度数；不存在时为 {@code null}
     */
    public Double getDouble(String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof Number ? ((Number) value).doubleValue() : Double.valueOf(value.toString());
    }

    /**
     * 是否包含键。
     *
     * @param key 键
     * @return 包含返回 {@code true}
     */
    public boolean containsKey(String key) {
        return metadata.containsKey(key);
    }

    /**
     * 写入字符串值。
     *
     * @param key   键
     * @param value 值
     * @return 本实例
     */
    public Metadata put(String key, String value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * 写入 UUID 值。
     *
     * @param key   键
     * @param value 值
     * @return 本实例
     */
    public Metadata put(String key, UUID value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * 写入整数值。
     *
     * @param key   键
     * @param value 值
     * @return 本实例
     */
    public Metadata put(String key, int value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * 写入长整数值。
     *
     * @param key   键
     * @param value 值
     * @return 本实例
     */
    public Metadata put(String key, long value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * 写入浮点值。
     *
     * @param key   键
     * @param value 值
     * @return 本实例
     */
    public Metadata put(String key, float value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * 写入双精度值。
     *
     * @param key   键
     * @param value 值
     * @return 本实例
     */
    public Metadata put(String key, double value) {
        metadata.put(key, value);
        return this;
    }

    /**
     * 批量写入。
     *
     * @param metadata 待写入键值
     * @return 本实例
     */
    public Metadata putAll(Map<String, Object> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }

    /**
     * 删除键。
     *
     * @param key 键
     * @return 本实例
     */
    public Metadata remove(String key) {
        metadata.remove(key);
        return this;
    }

    /**
     * 复制一份。
     *
     * @return 新实例
     */
    public Metadata copy() {
        return new Metadata(metadata);
    }

    /**
     * 转为 Map。
     *
     * @return 只读视图
     */
    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Metadata)) {
            return false;
        }
        return metadata.equals(((Metadata) o).metadata);
    }

    @Override
    public int hashCode() {
        return metadata.hashCode();
    }

    @Override
    public String toString() {
        return "Metadata " + metadata;
    }

    /**
     * 由单个键值创建。
     *
     * @param key   键
     * @param value 值
     * @return 元数据
     */
    public static Metadata from(String key, String value) {
        return new Metadata().put(key, value);
    }

    /**
     * 由 Map 创建。
     *
     * @param metadata 键值
     * @return 元数据
     */
    public static Metadata from(Map<String, ?> metadata) {
        return new Metadata(metadata);
    }

    /**
     * 由单个键值创建（{@link #from(String, String)} 的别名）。
     *
     * @param key   键
     * @param value 值
     * @return 元数据
     */
    public static Metadata metadata(String key, String value) {
        return from(key, value);
    }

    /**
     * 合并：返回新实例，{@code other} 覆盖同名键。
     *
     * @param other 待合并元数据
     * @return 新实例
     */
    public Metadata merge(Metadata other) {
        Metadata merged = new Metadata(metadata);
        if (other != null) {
            merged.metadata.putAll(other.metadata);
        }
        return merged;
    }
}
