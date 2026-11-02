package com.xiongx9527.fw.plus.ai.model.embedding.request;

import java.util.Objects;

/**
 * 嵌入请求的可选参数声明。
 *
 * <p><b>作用</b>：用「名称 + 类型」描述一个可选参数（模型名、维度、输入用途…），
 * 让请求参数可以按声明式方式读取与合并，而不必为每个参数写一套 getter。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.embedding.request.EmbeddingParameter}
 * （官方为 record；本实现按模块 source 级别改写为等价不可变类，成员语义一致）。</p>
 *
 * <p><b>实现约定</b>：不可变；{@link #name()} 与 {@link #type()} 均非空。</p>
 *
 * @param <T> 参数值类型
 * @author xiongxiao
 */
public final class EmbeddingParameter<T> {

    private final String name;
    private final Class<T> type;

    /**
     * 构造参数声明。
     *
     * @param name 参数名，不能为空白
     * @param type 参数值类型，不能为 {@code null}
     */
    public EmbeddingParameter(String name, Class<T> type) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        this.name = name;
        this.type = type;
    }

    /**
     * 参数名。
     *
     * @return 参数名
     */
    public String name() {
        return name;
    }

    /**
     * 参数值类型。
     *
     * @return 类型
     */
    public Class<T> type() {
        return type;
    }

    /**
     * 把任意值强转为本参数声明的类型。
     *
     * @param value 参数值，可为 {@code null}
     * @return 强转后的值；入参为 {@code null} 时返回 {@code null}
     * @throws IllegalArgumentException 值类型与本参数声明不符时抛出
     */
    public T cast(Object value) {
        if (value == null) {
            return null;
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Parameter '" + name + "' expects " + type.getName()
                    + " but was " + value.getClass().getName());
        }
        return type.cast(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EmbeddingParameter)) {
            return false;
        }
        EmbeddingParameter<?> that = (EmbeddingParameter<?>) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "EmbeddingParameter{name='" + name + "', type=" + type.getName() + "}";
    }
}
