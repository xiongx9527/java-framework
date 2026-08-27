package com.xiongx9527.fw.plus.mf.datachange;

import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryTable;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.table.ColumnInfo;
import com.mybatisflex.core.table.IdInfo;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import org.apache.ibatis.binding.MapperMethod.ParamMap;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class MybatisFlexEntityTypeResolver {
	private static final String[] ENTITY_KEYS = {"$$entity", "entity", "et", "record", "param1", "arg0"};
	private static final String[] QUERY_KEYS = {"$$query", "queryWrapper", "wrapper", "ew"};

	Class<?> resolve(DataChangeInvocation invocation) {
		if (invocation == null) { return null; }
		MapperMetadata mapperMetadata = mapperMetadata(invocation.getMappedStatement());
		if (mapperMetadata == null) { return null; }
		Class<?> entityType = directEntityType(invocation.getParameterObject());
		if (entityType != null) { return entityType; }
		entityType = wrapperEntityType(invocation.getParameterObject());
		if (entityType != null) { return entityType; }
		return validEntityType(mapperMetadata.entityType);
	}

	private Class<?> directEntityType(Object parameterObject) {
		if (!(parameterObject instanceof Map)) {
			return parameterObject instanceof QueryWrapper ? null : validEntityType(parameterObject == null
					? null : parameterObject.getClass());
		}
		if (!(parameterObject instanceof ParamMap)) { return null; }
		Map<?, ?> parameters = (Map<?, ?>) parameterObject;
		for (String key : ENTITY_KEYS) {
			if (!parameters.containsKey(key)) { continue; }
			Object candidate = parameters.get(key);
			Class<?> entityType = candidate == null || candidate instanceof QueryWrapper
					? null : validEntityType(candidate.getClass());
			if (entityType != null) { return entityType; }
		}
		return null;
	}

	private Class<?> wrapperEntityType(Object parameterObject) {
		if (parameterObject instanceof QueryWrapper) {
			return wrapperEntityType((QueryWrapper) parameterObject);
		}
		if (!(parameterObject instanceof ParamMap)) { return null; }
		Map<?, ?> parameters = (Map<?, ?>) parameterObject;
		for (String key : QUERY_KEYS) {
			if (!parameters.containsKey(key)) { continue; }
			Object candidate = parameters.get(key);
			if (!(candidate instanceof QueryWrapper)) { continue; }
			Class<?> entityType = wrapperEntityType((QueryWrapper) candidate);
			if (entityType != null) { return entityType; }
		}
		return null;
	}

	private Class<?> wrapperEntityType(QueryWrapper wrapper) {
		MetaObject metaObject = SystemMetaObject.forObject(wrapper);
		if (metaObject.hasGetter("entityClass")) {
			Object entityClass = metaObject.getValue("entityClass");
			Class<?> entityType = entityClass instanceof Class ? validEntityType((Class<?>) entityClass) : null;
			if (entityType != null) { return entityType; }
		}
		if (!metaObject.hasGetter("queryTables")) { return null; }
		Object queryTables = metaObject.getValue("queryTables");
		if (!(queryTables instanceof List)) { return null; }
		List<?> tables = (List<?>) queryTables;
		if (tables.size() != 1 || !(tables.get(0) instanceof QueryTable)) { return null; }
		QueryTable table = (QueryTable) tables.get(0);
		Class<?> schemaEntityType = tableEntityType(table.getNameWithSchema());
		Class<?> simpleEntityType = tableEntityType(table.getName());
		if (schemaEntityType == null) { return simpleEntityType; }
		if (simpleEntityType == null) { return schemaEntityType; }
		return schemaEntityType == simpleEntityType ? schemaEntityType : null;
	}

	private Class<?> tableEntityType(String tableName) {
		TableInfo tableInfo = TableInfoFactory.ofTableName(tableName);
		return tableInfo == null ? null : validEntityType(tableInfo.getEntityClass());
	}

	private Class<?> validEntityType(Class<?> entityType) {
		if (entityType == null || entityType.isArray() || entityType.isPrimitive()
				|| Map.class.isAssignableFrom(entityType) || Collection.class.isAssignableFrom(entityType)
				|| CharSequence.class.isAssignableFrom(entityType) || Number.class.isAssignableFrom(entityType)
				|| Boolean.class == entityType || Character.class == entityType || Class.class == entityType) {
			return null;
		}
		try {
			TableInfo tableInfo = TableInfoFactory.ofEntityClass(entityType);
			if (tableInfo == null || tableInfo.getEntityClass() != entityType) { return null; }
			List<ColumnInfo> columns = tableInfo.getColumnInfoList();
			if (columns != null) {
				for (ColumnInfo column : columns) {
					if (!column.isIgnore()) { return entityType; }
				}
			}
			List<IdInfo> primaryKeys = tableInfo.getPrimaryKeyList();
			if (primaryKeys != null) {
				for (IdInfo primaryKey : primaryKeys) {
					if (!primaryKey.isIgnore()) { return entityType; }
				}
			}
			return null;
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	private MapperMetadata mapperMetadata(MappedStatement mappedStatement) {
		if (mappedStatement == null || mappedStatement.getId() == null) { return null; }
		String id = mappedStatement.getId();
		int separator = id.lastIndexOf('.');
		if (separator <= 0) { return null; }
		try {
			Class<?> mapperType = Resources.classForName(id.substring(0, separator));
			if (!BaseMapper.class.isAssignableFrom(mapperType)) { return null; }
			return new MapperMetadata(resolveBaseMapperType(mapperType,
					Collections.<TypeVariable<?>, Type>emptyMap()));
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

	private static final class MapperMetadata {
		private final Class<?> entityType;

		private MapperMetadata(Class<?> entityType) {
			this.entityType = entityType;
		}
	}
}
