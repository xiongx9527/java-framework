package com.xiongx9527.fw.plus.mp.datachange;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class MybatisPlusEntityTypeResolver {

	Class<?> resolve(DataChangeInvocation invocation) {
		if (invocation == null) { return null; }
		Object parameterObject = invocation.getParameterObject();
		Class<?> directEntityType = directEntityType(parameterObject);
		if (directEntityType != null) { return directEntityType; }
		Class<?> wrapperEntityType = wrapperEntityType(parameterObject);
		if (wrapperEntityType != null) { return wrapperEntityType; }
		return mapperEntityType(invocation.getMappedStatement());
	}

	private Class<?> directEntityType(Object parameterObject) {
		Object candidate = parameterObject;
		if (parameterObject instanceof Map) {
			Map<?, ?> parameters = (Map<?, ?>) parameterObject;
			candidate = parameters.containsKey(Constants.ENTITY) ? parameters.get(Constants.ENTITY) : null;
		}
		if (candidate == null || candidate instanceof Wrapper) { return null; }
		return TableInfoHelper.getTableInfo(candidate.getClass()) == null ? null : candidate.getClass();
	}

	private Class<?> wrapperEntityType(Object parameterObject) {
		Wrapper<?> wrapper = wrapper(parameterObject);
		if (wrapper == null) { return null; }
		if (wrapper.getEntity() != null) { return wrapper.getEntity().getClass(); }
		MetaObject metaObject = SystemMetaObject.forObject(wrapper);
		if (!metaObject.hasGetter("entityClass")) { return null; }
		Object entityClass = metaObject.getValue("entityClass");
		return entityClass instanceof Class ? (Class<?>) entityClass : null;
	}

	private Wrapper<?> wrapper(Object parameterObject) {
		if (parameterObject instanceof Wrapper) { return (Wrapper<?>) parameterObject; }
		if (!(parameterObject instanceof Map)) { return null; }
		Map<?, ?> parameters = (Map<?, ?>) parameterObject;
		Object candidate = parameters.containsKey(Constants.WRAPPER) ? parameters.get(Constants.WRAPPER) : null;
		return candidate instanceof Wrapper ? (Wrapper<?>) candidate : null;
	}

	private Class<?> mapperEntityType(MappedStatement mappedStatement) {
		if (mappedStatement == null || mappedStatement.getId() == null) { return null; }
		String id = mappedStatement.getId();
		int separator = id.lastIndexOf('.');
		if (separator <= 0) { return null; }
		try {
			Class<?> mapperType = Resources.classForName(id.substring(0, separator));
			return resolveBaseMapperType(mapperType, Collections.<TypeVariable<?>, Type>emptyMap());
		} catch (ClassNotFoundException ignored) {
			return null;
		}
	}

	private Class<?> resolveBaseMapperType(Type type, Map<TypeVariable<?>, Type> inheritedTypes) {
		Class<?> rawType;
		Map<TypeVariable<?>, Type> resolvedTypes = new HashMap<TypeVariable<?>, Type>(inheritedTypes);
		if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			if (!(parameterizedType.getRawType() instanceof Class)) { return null; }
			rawType = (Class<?>) parameterizedType.getRawType();
			TypeVariable<?>[] variables = rawType.getTypeParameters();
			Type[] arguments = parameterizedType.getActualTypeArguments();
			for (int index = 0; index < variables.length; index++) {
				resolvedTypes.put(variables[index], resolveType(arguments[index], inheritedTypes));
			}
			if (rawType == BaseMapper.class) {
				return asClass(resolveType(arguments[0], inheritedTypes));
			}
		} else if (type instanceof Class) {
			rawType = (Class<?>) type;
		} else {
			return null;
		}
		for (Type interfaceType : rawType.getGenericInterfaces()) {
			Class<?> entityType = resolveBaseMapperType(interfaceType, resolvedTypes);
			if (entityType != null) { return entityType; }
		}
		Type superclass = rawType.getGenericSuperclass();
		return superclass == null ? null : resolveBaseMapperType(superclass, resolvedTypes);
	}

	private Type resolveType(Type type, Map<TypeVariable<?>, Type> resolvedTypes) {
		Type resolved = type;
		while (resolved instanceof TypeVariable && resolvedTypes.containsKey(resolved)) {
			Type next = resolvedTypes.get(resolved);
			if (next == resolved) { break; }
			resolved = next;
		}
		return resolved;
	}

	private Class<?> asClass(Type type) {
		if (type instanceof Class) { return (Class<?>) type; }
		if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() instanceof Class) {
			return (Class<?>) ((ParameterizedType) type).getRawType();
		}
		return null;
	}
}
