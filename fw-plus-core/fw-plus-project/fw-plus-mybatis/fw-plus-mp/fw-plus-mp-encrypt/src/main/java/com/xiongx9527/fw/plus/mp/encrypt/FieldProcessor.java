package com.xiongx9527.fw.plus.mp.encrypt;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.xiongx9527.fw.plus.common.exception.BaseRuntimeException;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;

public class FieldProcessor {

    /**
     * 字段设置属性缓存映射表
     * 用于存储类与其对应的字段设置属性列表的映射关系
     * key: 类对象
     * value: 该类所有字段的设置属性列表
     */
    private static Map<Class<?>, List<FieldSetProperty>> fieldSetCache = new ConcurrentHashMap<>(16);

    /**
     * 已处理类集合
     * 用于记录已经进行过字段处理的类，避免重复处理
     * 使用线程安全的CopyOnWriteArraySet确保并发安全性
     */
    private static Set<Class<?>> processedClasses = new CopyOnWriteArraySet<>();


    /**
     * 私有构造函数，用于创建FieldProcessor实例
     * 此构造函数不接受任何参数，也不返回任何值
     * 由于是私有构造函数，只能在类内部调用
     */
    private FieldProcessor() {
    }

    /**
     * 初始化字段处理器配置
     *
     * @param enableFieldEncrypt 是否启用字段加密
     * @param enableFieldBind    是否启用字段绑定
     */

    /**
    public static void initialize(boolean enableFieldEncrypt, boolean enableFieldBind) {
        isFieldEncryptEnabled = enableFieldEncrypt;
        isFieldBindEnabled = enableFieldBind;
        fieldSetCache = new ConcurrentHashMap<>(16);
        processedClasses = new CopyOnWriteArraySet<>();
    }
     **/

    /**
     * 获取所有可访问的字段，包括所有继承层次的私有和受保护字段
     * 此方法通过遍历类及其超类来收集所有字段，并将它们设置为可访问状态，以便反射机制可以访问这些字段
     *
     * @param clazz 要检查的类对象，从这个类开始收集字段
     * @return 包含所有可访问字段的列表
     */
    private static List<Field> getAccessibleFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    /**
     * 获取类的字段属性列表
     *
     * @param clazz 类对象
     * @return 字段属性列表
     */
    public static List<FieldSetProperty> getFieldSetProperties(Class<?> clazz) {
        if (processedClasses.contains(clazz)) {
            return fieldSetCache.get(clazz);
        }
        List<FieldSetProperty> fieldSetProperties = fieldSetCache.get(clazz);
        if (CollectionUtils.isEmpty(fieldSetProperties)) {
            if (clazz.isAssignableFrom(HashMap.class)) {
                processedClasses.add(clazz);
            } else {
                fieldSetProperties = new ArrayList<>();
                for (Field field : getAccessibleFields(clazz)) {
                    FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
                    if (fieldEncrypt != null && !field.getType().isAssignableFrom(String.class)) {
                        throw new BaseRuntimeException("annotation `@FieldEncrypt` only string types are supported.");
                    }
                    if (fieldEncrypt != null) {
                        TableField tableField = field.getAnnotation(TableField.class);
                        String fielName = field.getName();
                        if (null != tableField && StringUtils.isNotEmpty(tableField.value())) {
                            fielName = tableField.value();
                        }
                        fieldSetProperties.add(new FieldSetProperty(fielName, fieldEncrypt));
                    }
                }
                if (fieldSetProperties.isEmpty()) {
                    processedClasses.add(clazz);
                    fieldSetCache.put(clazz, fieldSetProperties);
                }
            }
        }

        return fieldSetProperties;
    }

    /**
     * 处理对象的字段属性
     *
     * @param configuration MyBatis 配置对象
     * @param target        目标对象
     * @param processor     字段处理器
     * @return 是否成功处理
     */
    public static boolean processFields(Configuration configuration, Object target, BiConsumer<MetaObject, FieldSetProperty> processor) {
        List<FieldSetProperty> fieldSetProperties = getFieldSetProperties(target.getClass());
        if (CollectionUtils.isNotEmpty(fieldSetProperties)) {
            MetaObject metaObject = configuration.newMetaObject(target);
            fieldSetProperties.parallelStream().forEach(property -> processor.accept(metaObject, property));
            return true;
        } else {
            return false;
        }
    }

    /**
     * 处理对象的加密字段属性
     *
     * @param configuration MyBatis 配置对象
     * @param target        目标对象
     * @param processor     字段处理器
     * @return 是否成功处理
     */
    public static boolean processEncryptedFields(Configuration configuration, Object target, BiConsumer<MetaObject, FieldSetProperty> processor) {
        if (target != null) {
            List<FieldSetProperty> fieldSetProperties = getFieldSetProperties(target.getClass());
            if (CollectionUtils.isNotEmpty(fieldSetProperties) && fieldSetProperties.stream().anyMatch(property -> property.getFieldEncrypt() != null)) {
                MetaObject metaObject = configuration.newMetaObject(target);
                fieldSetProperties.stream()
                        .filter(property -> property.getFieldEncrypt() != null)
                        .forEach(property -> processor.accept(metaObject, property));
                return true;
            }
        }
        return false;
    }
}