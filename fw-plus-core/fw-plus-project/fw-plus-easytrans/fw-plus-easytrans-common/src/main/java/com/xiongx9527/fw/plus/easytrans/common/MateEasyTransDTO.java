package com.xiongx9527.fw.plus.easytrans.common;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dromara.core.trans.util.ReflectUtils;
import org.dromara.core.trans.vo.TransPojo;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 抽象类，用于实现通用的转换功能。
 *
 */
public abstract class MateEasyTransDTO implements TransPojo {
    /**
     * 获取实体的唯一值
     * 此方法用于获取实体对象的唯一标识值，例如主键值
     * 该方法标记为 {@code @JsonIgnore} 和 {@code @JSONField(serialize = false)}，表示在序列化为 JSON 时忽略此方法
     * 这是因为唯一值通常是内部使用的，不应该暴露给 API 的消费者
     *
     * @return Object 返回唯一值，通常为一个标识主键的对象，如 Long 或 String
     */
    @JsonIgnore
    @JSONField(serialize = false)
    protected Object getUniqueValue() {
        return null;
    }

    /**
     * 重写获取转换映射的方法
     * 该方法用于获取当前对象的转换映射，如果缓存中不存在，则会创建一个新的映射并存入缓存
     * 如果对象的唯一键（pkey）为空，则会抛出运行时异常
     *
     * @return 包含转换信息的映射对象
     * @throws RuntimeException 如果未找到对象的唯一键，则抛出此异常
     */
    @Override
    @JsonIgnore
    @JSONField(serialize = false)
    public Map<String, Object> getTransMap() {
        // 检查缓存是否已初始化，未初始化则清除转换缓存
        if (TRANS_MAP_CACHE.get() == null) {
            this.clearTransCache();
        }
        // 从缓存中获取当前类的转换映射
        Map<String, Map<String, Object>> cache = (Map) TRANS_MAP_CACHE.get();
        // 获取对象的唯一键
        Object pkey = this.getPkey();
        // 如果唯一键为空，则抛出异常提示未设置唯一键
        if (Objects.isNull(pkey)) {
            throw new RuntimeException(String.format("未找到%s的唯一key,请设置id或者实现getUniqueValue方法", this.getClass().getName()));
        }
        // 构造缓存键，包含类名和唯一键，以确保键的唯一性
        String cacheKey = this.getClass().getName() + StrUtil.UNDERLINE + pkey;
        // 如果缓存中已存在该键对应的映射，则直接返回
        if (cache.containsKey(cacheKey)) {
            return (Map) cache.get(cacheKey);
        } else {
            // 如果缓存中不存在该键对应的映射，则创建一个新的映射对象
            Map<String, Object> result = new LinkedHashMap();
            // 将新的映射对象存入缓存
            cache.put(cacheKey, result);
            // 返回新的映射对象
            return result;
        }
    }

    /**
     * 重写获取唯一键值的方法
     * 该方法用于获取对象的唯一标识键值，主要用于数据的唯一性识别和关联
     * 使用注解忽略此方法以防止在JSON序列化和反序列化时被错误处理
     *
     * @return Object 返回唯一的键值对象，如果找不到则抛出运行时异常
     * @throws RuntimeException 如果无法找到唯一键值，则抛出此异常，并建议设置id或实现getUniqueValue方法
     */
    @Override
    @JsonIgnore
    @JSONField(serialize = false)
    public final Object getPkey() {
        try {
            // 尝试获取对象的唯一值
            Object uniqueValue = getUniqueValue();
            // 如果唯一值非空，则直接返回该值
            if (Objects.nonNull(uniqueValue)) {
                return uniqueValue;
            }
            // 如果唯一值为空，尝试通过反射获取id字段
            Field idField = ReflectUtils.getIdField(this.getClass(), true);
            // 返回id字段的值
            return idField.get(this);
        } catch (Throwable e) {
            // 如果发生异常，抛出运行时异常，指示未找到唯一的key，并建议设置id或者实现getUniqueValue方法
            throw new RuntimeException(String.format("未找到%s的唯一key,请设置id或者实现getUniqueValue方法", this.getClass().getName()), e);
        }
    }
}