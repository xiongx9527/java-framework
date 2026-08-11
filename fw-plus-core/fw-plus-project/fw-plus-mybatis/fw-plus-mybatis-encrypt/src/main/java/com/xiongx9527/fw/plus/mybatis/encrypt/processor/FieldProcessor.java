package com.xiongx9527.fw.plus.mybatis.encrypt.processor;

import com.xiongx9527.fw.plus.common.exception.BaseRuntimeException;
import com.xiongx9527.fw.plus.common.mate.annotation.FieldEncrypt;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldNameResolver;
import com.xiongx9527.fw.plus.mybatis.encrypt.metadata.FieldSetProperty;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * 字段元数据解析与处理。
 *
 * <p>反射收集带 {@code @FieldEncrypt} 注解的字段（含继承层次），校验类型约束，
 * 并通过 {@link FieldNameResolver} 解析列名映射。缓存按实体类型隔离，同一类型
 * 在同一 resolver 下解析结果一致。</p>
 */
public class FieldProcessor {

	private final FieldNameResolver fieldNameResolver;
	private final Map<Class<?>, List<FieldSetProperty>> fieldSetCache = new ConcurrentHashMap<Class<?>, List<FieldSetProperty>>();

	public FieldProcessor(FieldNameResolver fieldNameResolver) {
		this.fieldNameResolver = fieldNameResolver;
	}

	/**
	 * 获取类上带 {@code @FieldEncrypt} 注解的字段属性列表。
	 *
	 * @param clazz 实体类型
	 * @return 字段属性列表；HashMap 及其父类类型返回 {@code null}
	 */
	public List<FieldSetProperty> getFieldSetProperties(Class<?> clazz) {
		if (clazz.isAssignableFrom(HashMap.class)) {
			return null;
		}
		List<FieldSetProperty> cached = fieldSetCache.get(clazz);
		if (cached != null) {
			return cached;
		}
		List<FieldSetProperty> fieldSetProperties = new ArrayList<FieldSetProperty>();
		for (Field field : getAccessibleFields(clazz)) {
			FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
			if (fieldEncrypt != null && !field.getType().isAssignableFrom(String.class)) {
				throw new BaseRuntimeException("annotation `@FieldEncrypt` only string types are supported.");
			}
			if (fieldEncrypt != null) {
				fieldSetProperties.add(new FieldSetProperty(field.getName(), resolveColumnName(clazz, field),
						fieldEncrypt));
			}
		}
		fieldSetCache.put(clazz, fieldSetProperties);
		return fieldSetProperties;
	}

	/**
	 * 处理对象中的加密字段。
	 *
	 * @param configuration MyBatis 配置
	 * @param target        目标对象
	 * @param processor     字段处理器
	 * @return 是否处理了字段
	 */
	public boolean processFields(Configuration configuration, Object target,
			BiConsumer<MetaObject, FieldSetProperty> processor) {
		List<FieldSetProperty> fieldSetProperties = getFieldSetProperties(target.getClass());
		if (CollectionUtils.isNotEmpty(fieldSetProperties)) {
			MetaObject metaObject = configuration.newMetaObject(target);
			fieldSetProperties.parallelStream().forEach(property -> processor.accept(metaObject, property));
			return true;
		}
		return false;
	}

	/**
	 * 处理对象中带 {@code @FieldEncrypt} 注解的字段。
	 *
	 * @param configuration MyBatis 配置
	 * @param target        目标对象
	 * @param processor     字段处理器
	 * @return 是否处理了字段
	 */
	public boolean processEncryptedFields(Configuration configuration, Object target,
			BiConsumer<MetaObject, FieldSetProperty> processor) {
		if (target != null) {
			List<FieldSetProperty> fieldSetProperties = getFieldSetProperties(target.getClass());
			if (CollectionUtils.isNotEmpty(fieldSetProperties)
					&& fieldSetProperties.stream().anyMatch(property -> property.getFieldEncrypt() != null)) {
				MetaObject metaObject = configuration.newMetaObject(target);
				fieldSetProperties.stream()
						.filter(property -> property.getFieldEncrypt() != null)
						.forEach(property -> processor.accept(metaObject, property));
				return true;
			}
		}
		return false;
	}

	private String resolveColumnName(Class<?> clazz, Field field) {
		if (fieldNameResolver == null) {
			return null;
		}
		return fieldNameResolver.resolveColumnName(clazz, field.getName());
	}

	private static List<Field> getAccessibleFields(Class<?> clazz) {
		List<Field> fields = new ArrayList<Field>();
		while (clazz != null) {
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				fields.add(field);
			}
			clazz = clazz.getSuperclass();
		}
		return fields;
	}
}
