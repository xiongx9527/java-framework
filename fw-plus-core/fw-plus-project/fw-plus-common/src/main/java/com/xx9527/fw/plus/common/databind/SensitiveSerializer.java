package com.xx9527.fw.plus.common.databind;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.xx9527.fw.plus.common.mate.annotation.FieldSensitive;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author xiongxiao
 */
@Slf4j
public class SensitiveSerializer extends JsonSerializer<String> implements JsonContextualSerializer {
    /**
     * 敏感策略接口实例，用于处理敏感信息相关策略
     */
    private static volatile ISensitiveStrategy SENSITIVE_STRATEGY;

    /**
     * 类型字段，表示当前对象的类型标识
     */
    private String type;

    /**
     * 默认构造函数，创建一个SensitiveSerializer实例
     */
    public SensitiveSerializer() {
    }

    /**
     * 带类型参数的构造函数，创建一个SensitiveSerializer实例并设置序列化类型
     * @param type 序列化类型字符串
     */
    public SensitiveSerializer(String type) {
        this.type = type;
    }


    /**
     * 设置敏感信息处理策略
     *
     * 该方法用于动态设置敏感信息处理策略，通过传入一个实现了ISensitiveStrategy接口的策略对象，
     * 来决定如何处理敏感信息这允许程序在运行时根据需要灵活地更改敏感信息的处理方式，
     * 提高了代码的可扩展性和可维护性
     *
     * @param sensitiveStrategy 实现了ISensitiveStrategy接口的策略对象，用于定义如何处理敏感信息
     */
    public static void setSensitiveStrategy(ISensitiveStrategy sensitiveStrategy) {
        SENSITIVE_STRATEGY = sensitiveStrategy;
    }

    /**
     * 重写serialize方法以实现自定义的敏感信息处理逻辑
     *
     * @param value 待序列化的字符串值
     * @param gen 用于生成JSON数据的工具类
     * @param serializers 提供序列化器的工具类
     * @throws IOException 当JSON生成过程中发生I/O错误时抛出
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 检查是否已经注入了敏感信息处理策略
        if (null == SENSITIVE_STRATEGY) {
            // 如果未注入敏感信息处理策略，则抛出运行时异常
            //throw new RuntimeException("You used the annotation `@FieldSensitive` but did not inject `SensitiveStrategy`");
            log.warn("You used the annotation `@FieldSensitive` but did not inject `SensitiveStrategy`");
        } else {
            // 初始化字段值为null，用于后续判断是否需要进行敏感信息处理
            Object fieldValue = null;
            // 获取当前请求中跳过敏感信息处理的字段名
            Object fieldNames = RequestDataTransfer.get(RequestDataTransfer.SKIP_SENSITIVE);
            // 如果字段名是"_all_"，则表示所有字段都需要进行敏感信息处理
            if (Objects.equals("_all_", fieldNames)) {
                fieldValue = value;
            } else {
                // 如果字段名不为null且不为"_all_"，则进一步判断当前字段是否需要跳过敏感信息处理
                if (null != fieldNames) {
                    // 获取当前JSON生成上下文
                    JsonStreamContext jsc = gen.getOutputContext();
                    // 如果上下文不为null，获取当前字段名，并判断是否需要跳过敏感信息处理
                    if (null != jsc) {
                        String currentName = jsc.getCurrentName();
	                    String fieldNames1 = (String) fieldNames;
	                    List<String> fieldList = Arrays.asList(fieldNames1.split(","));
	                    if (null != currentName && fieldList.contains(currentName)) {
                            fieldValue = value;
                        }
                    }
                }

                // 如果字段值仍为null，表示当前字段需要进行敏感信息处理
                if (null == fieldValue) {
                    // 使用敏感信息处理策略处理当前字段值
                    fieldValue = SENSITIVE_STRATEGY.handle(this.type, value);
                }
            }

            // 将处理后的字段值写入JSON
            gen.writeObject(fieldValue);
        }
    }

    /**
     * 自定义Json序列化器获取方法
     * 该方法用于根据字段类型和注解动态选择合适的Json序列化器
     * 主要用于处理敏感信息的字段，使其在序列化时能够按照指定的方式进行处理
     *
     * @param provider 序列化器提供者，用于获取默认序列化器
     * @param property 正在处理的Bean属性，用于获取字段类型和注解信息
     * @return 返回自定义的Json序列化器实例，如果不需要自定义处理，则返回默认序列化器
     * @throws JsonMappingException 如果序列化器获取过程中发生错误
     */
    @Override
    public JsonSerializer<?> getJsonSerializer(SerializerProvider provider, BeanProperty property) throws JsonMappingException {
        // 检查当前属性的类型是否为String，因为敏感信息通常用String表示
        if (Objects.equals(property.getType().getRawClass(), String.class)) {
            // 尝试获取当前属性上的FieldSensitive注解
            FieldSensitive sensitiveInfo = (FieldSensitive)this.getAnnotation(property, FieldSensitive.class);
            // 如果该属性上有FieldSensitive注解，则创建并返回一个SensitiveSerializer
            if (null != sensitiveInfo) {
                return new SensitiveSerializer(sensitiveInfo.value());
            }
        }

        // 如果属性类型不是String或者没有FieldSensitive注解，则返回默认的序列化器
        return provider.findValueSerializer(property.getType(), property);
    }
}