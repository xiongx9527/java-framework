package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.GeneratedKeyAccessor;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class GeneratedKeyResolver {
	private final DataChangeDialect dialect;

	public GeneratedKeyResolver() {
		this(null);
	}

	public GeneratedKeyResolver(DataChangeDialect dialect) {
		this.dialect = dialect;
	}

	public List<RowIdentity> resolve(MappedStatement mappedStatement, Object parameterObject, Statement statement,
			FrameworkChangeMetadata frameworkMetadata, List<String> keyColumns) {
		if (mappedStatement == null || mappedStatement.getSqlCommandType() != SqlCommandType.INSERT
				|| statement == null) {
			throw unavailable();
		}
		List<String> keys = validKeys(keyColumns);
		GeneratedKeyAccessor accessor = frameworkMetadata == null ? null
				: frameworkMetadata.getGeneratedKeyAccessor();
		List<Object> parameters;
		try {
			parameters = insertionParameters(parameterObject, mappedStatement);
		} catch (RuntimeException exception) {
			throw unavailable();
		}
		if (parameters.isEmpty()) {
			throw unavailable();
		}
		List<Map<String, Object>> existing = new ArrayList<Map<String, Object>>();
		boolean allComplete = true;
		for (Object parameter : parameters) {
			Map<String, Object> values;
			try {
				values = directValues(parameter, mappedStatement, keys);
			} catch (RuntimeException exception) {
				values = new LinkedHashMap<String, Object>();
			}
			if (!complete(values, keys) && accessor != null) {
				try {
					Map<String, Object> adapted = ordered(accessor.readKeyValues(parameter, keys), keys, false);
					for (String key : keys) {
						if ((!values.containsKey(key) || values.get(key) == null) && adapted.containsKey(key)) {
							values.put(key, adapted.get(key));
						}
					}
				} catch (RuntimeException ignored) {
					// Continue with JDBC generated keys without exposing adapter details.
				}
			}
			existing.add(values);
			allComplete &= complete(values, keys);
		}
		if (allComplete) {
			return identities(existing);
		}
		try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
			if (generatedKeys == null) {
				throw unavailable();
			}
			ResultSetMetaData metadata = generatedKeys.getMetaData();
			List<RowIdentity> identities = new ArrayList<RowIdentity>();
			Set<RowIdentity> unique = new HashSet<RowIdentity>();
			for (int index = 0; index < parameters.size(); index++) {
				if (!generatedKeys.next()) {
					throw unavailable();
				}
				Map<String, Object> values = existing.get(index);
				if (!complete(values, keys)) {
					Map<String, Object> jdbcValues = jdbcValues(generatedKeys, metadata, keys);
					for (String key : keys) {
						if (!values.containsKey(key) || values.get(key) == null) {
							values.put(key, jdbcValues.get(key));
						}
					}
				}
				if (!complete(values, keys)) {
					throw unavailable();
				}
				RowIdentity identity = new RowIdentity(ordered(values, keys, true));
				if (!unique.add(identity)) {
					throw unavailable();
				}
				identities.add(identity);
			}
			if (generatedKeys.next()) {
				throw unavailable();
			}
			return Collections.unmodifiableList(identities);
		} catch (RuntimeException exception) {
			return dialectLookup(mappedStatement, parameterObject, statement, frameworkMetadata, keys,
					parameters.size());
		} catch (SQLException exception) {
			return dialectLookup(mappedStatement, parameterObject, statement, frameworkMetadata, keys,
					parameters.size());
		}
	}

	private List<RowIdentity> dialectLookup(MappedStatement mappedStatement, Object parameterObject,
			Statement statement, FrameworkChangeMetadata frameworkMetadata, List<String> keys, int parameterCount) {
		if (dialect == null) { throw unavailable(); }
		try {
			List<RowIdentity> resolved = dialect.lookupGeneratedKeys(mappedStatement, parameterObject, statement,
					frameworkMetadata, keys);
			if (resolved == null || resolved.size() != parameterCount) { throw unavailable(); }
			List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
			for (RowIdentity identity : resolved) {
				if (identity == null) { throw unavailable(); }
				Map<String, Object> ordered = ordered(identity.getKeyValues(), keys, true);
				if (!complete(ordered, keys)) { throw unavailable(); }
				values.add(ordered);
			}
			return identities(values);
		} catch (RuntimeException exception) {
			throw unavailable();
		}
	}

	private Map<String, Object> directValues(Object parameter, MappedStatement mappedStatement, List<String> keys) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		if (parameter == null) { return values; }
		if (parameter instanceof Map) {
			Map<?, ?> source = (Map<?, ?>) parameter;
			for (String key : keys) {
				for (Map.Entry<?, ?> entry : source.entrySet()) {
					if (entry.getKey() instanceof String && key.equalsIgnoreCase((String) entry.getKey())) {
						values.put(key, entry.getValue());
						break;
					}
				}
			}
		} else {
			MetaObject metaObject = SystemMetaObject.forObject(parameter);
			for (String key : keys) {
				if (metaObject.hasGetter(key)) {
					values.put(key, metaObject.getValue(key));
				}
			}
		}
		addMappedKeyProperties(values, parameter, mappedStatement, keys);
		return values;
	}

	private void addMappedKeyProperties(Map<String, Object> values, Object parameter,
			MappedStatement mappedStatement, List<String> keys) {
		String[] properties = mappedStatement.getKeyProperties();
		String[] columns = mappedStatement.getKeyColumns();
		if (properties == null || properties.length == 0) { return; }
		if (columns != null && columns.length > 0 && columns.length != properties.length) { return; }
		if ((columns == null || columns.length == 0) && properties.length != keys.size()) { return; }
		for (int propertyIndex = 0; propertyIndex < properties.length; propertyIndex++) {
			String key = columns == null || columns.length == 0
					? keys.get(propertyIndex) : matchingKey(keys, columns[propertyIndex]);
			if (key == null) { continue; }
			if (values.containsKey(key) && values.get(key) != null) { continue; }
			Object value = propertyValue(parameter, properties[propertyIndex]);
			if (value != null) { values.put(key, value); }
		}
	}

	private String matchingKey(List<String> keys, String column) {
		if (column == null) { return null; }
		for (String key : keys) {
			if (key.equalsIgnoreCase(column)) { return key; }
		}
		return null;
	}

	private Object propertyValue(Object parameter, String property) {
		if (parameter == null || property == null || property.trim().isEmpty()) { return null; }
		MetaObject metaObject = SystemMetaObject.forObject(parameter);
		if (metaObject.hasGetter(property)) { return metaObject.getValue(property); }
		int separator = property.lastIndexOf('.');
		String itemProperty = separator < 0 ? property : property.substring(separator + 1);
		return metaObject.hasGetter(itemProperty) ? metaObject.getValue(itemProperty) : null;
	}

	private List<String> validKeys(List<String> keyColumns) {
		if (keyColumns == null || keyColumns.isEmpty()) {
			throw unavailable();
		}
		List<String> keys = new ArrayList<String>();
		for (String key : keyColumns) {
			if (key == null || key.trim().isEmpty() || containsIgnoreCase(keys, key)) {
				throw unavailable();
			}
			keys.add(key);
		}
		return keys;
	}

	private List<Object> insertionParameters(Object parameterObject, MappedStatement mappedStatement) {
		Object batch = batchValue(parameterObject, mappedStatement);
		if (batch instanceof Collection) {
			return new ArrayList<Object>((Collection<?>) batch);
		}
		if (batch != null && batch.getClass().isArray()) {
			List<Object> values = new ArrayList<Object>();
			for (int index = 0; index < Array.getLength(batch); index++) {
				values.add(Array.get(batch, index));
			}
			return values;
		}
		return Collections.singletonList(parameterObject);
	}

	private Object batchValue(Object parameterObject, MappedStatement mappedStatement) {
		if (parameterObject instanceof Collection || parameterObject != null && parameterObject.getClass().isArray()) {
			return parameterObject;
		}
		if (!(parameterObject instanceof Map)) {
			return null;
		}
		Map<?, ?> values = (Map<?, ?>) parameterObject;
		for (String alias : new String[] { "list", "collection", "array" }) {
			Object value = values.containsKey(alias) ? values.get(alias) : null;
			if (value instanceof Collection || value != null && value.getClass().isArray()) {
				return value;
			}
		}
		String[] keyProperties = mappedStatement.getKeyProperties();
		if (keyProperties != null) {
			for (String keyProperty : keyProperties) {
				int separator = keyProperty == null ? -1 : keyProperty.indexOf('.');
				if (separator <= 0) { continue; }
				String alias = keyProperty.substring(0, separator);
				Object value = values.containsKey(alias) ? values.get(alias) : null;
				if (value instanceof Collection || value != null && value.getClass().isArray()) {
					return value;
				}
			}
		}
		return null;
	}

	private Map<String, Object> jdbcValues(ResultSet resultSet, ResultSetMetaData metadata,
			List<String> keys) throws SQLException {
		Map<String, Integer> labels = new LinkedHashMap<String, Integer>();
		for (int index = 1; index <= metadata.getColumnCount(); index++) {
			labels.put(metadata.getColumnLabel(index).toLowerCase(Locale.ENGLISH), index);
		}
		boolean positional = metadata.getColumnCount() == keys.size();
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (int index = 0; index < keys.size(); index++) {
			String key = keys.get(index);
			Integer column = labels.get(key.toLowerCase(Locale.ENGLISH));
			if (column == null && positional) {
				column = index + 1;
			}
			if (column == null) {
				throw unavailable();
			}
			Object value = resultSet.getObject(column.intValue());
			if (value == null) {
				throw unavailable();
			}
			values.put(key, value);
		}
		return values;
	}

	private Map<String, Object> ordered(Map<String, Object> source, List<String> keys, boolean requireComplete) {
		Map<String, Object> ordered = new LinkedHashMap<String, Object>();
		if (source != null) {
			for (String key : keys) {
				String actual = actualKey(source, key);
				if (actual != null) {
					ordered.put(key, source.get(actual));
				} else if (requireComplete) {
					throw unavailable();
				}
			}
		}
		return ordered;
	}

	private String actualKey(Map<String, Object> values, String key) {
		for (String candidate : values.keySet()) {
			if (candidate != null && candidate.equalsIgnoreCase(key)) { return candidate; }
		}
		return null;
	}

	private boolean complete(Map<String, Object> values, List<String> keys) {
		for (String key : keys) {
			if (!values.containsKey(key) || values.get(key) == null) { return false; }
		}
		return true;
	}

	private boolean containsIgnoreCase(List<String> values, String candidate) {
		for (String value : values) {
			if (value.equalsIgnoreCase(candidate)) { return true; }
		}
		return false;
	}

	private List<RowIdentity> identities(List<Map<String, Object>> values) {
		try {
			List<RowIdentity> identities = new ArrayList<RowIdentity>();
			Set<RowIdentity> unique = new HashSet<RowIdentity>();
			for (Map<String, Object> value : values) {
				RowIdentity identity = new RowIdentity(value);
				if (!unique.add(identity)) {
					throw unavailable();
				}
				identities.add(identity);
			}
			return Collections.unmodifiableList(identities);
		} catch (RuntimeException exception) {
			throw unavailable();
		}
	}

	private DataChangeException unavailable() {
		return new DataChangeException(DataChangeErrorCode.GENERATED_KEY_UNAVAILABLE,
				"GENERATED_KEY_UNAVAILABLE");
	}

}
