package com.xiongx9527.fw.plus.ai.model.output.structured;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 结构化输出字段说明。
 *
 * <p><b>作用</b>：标注在 Java 字段 / 类上，为「让模型输出结构化对象」提供字段语义描述，
 * 由适配层转成 JSON Schema 的 {@code description}，提升模型填充准确率。</p>
 *
 * <p><b>出处</b>：逐字镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.output.structured.Description}。</p>
 *
 * <p><b>实现约定</b>：{@link #value()} 为字符串数组，多段内容按顺序拼接成一段描述；
 * 运行时保留（{@link RetentionPolicy#RUNTIME}），供 Schema 生成器反射读取。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Description {

    /**
     * 描述内容，多段按顺序拼接。
     *
     * @return 描述文本片段
     */
    String[] value();
}
