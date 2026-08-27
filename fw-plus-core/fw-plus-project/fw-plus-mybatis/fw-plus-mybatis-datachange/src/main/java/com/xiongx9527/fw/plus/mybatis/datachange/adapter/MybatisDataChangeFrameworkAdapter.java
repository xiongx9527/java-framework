package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.LogicalDeleteConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadata;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MybatisDataChangeFrameworkAdapter implements DataChangeFrameworkAdapter {
	private static final GeneratedKeyAccessor GENERATED_KEY_ACCESSOR = new GeneratedKeyAccessor() {
		@Override
		public Map<String, Object> readKeyValues(Object parameterObject, List<String> keyColumns) {
			Map<String, Object> keyValues = new LinkedHashMap<String, Object>();
			if (parameterObject == null || keyColumns == null) {
				return keyValues;
			}
			if (parameterObject instanceof Map) {
				Map<?, ?> values = (Map<?, ?>) parameterObject;
				for (String keyColumn : keyColumns) {
					if (values.containsKey(keyColumn)) {
						keyValues.put(keyColumn, values.get(keyColumn));
					}
				}
				return keyValues;
			}
			MetaObject values = SystemMetaObject.forObject(parameterObject);
			for (String keyColumn : keyColumns) {
				if (values.hasGetter(keyColumn)) {
					keyValues.put(keyColumn, values.getValue(keyColumn));
				}
			}
			return keyValues;
		}
	};

	@Override
	public boolean supports(DataChangeInvocation invocation) {
		if (invocation == null || invocation.getMappedStatement() == null) {
			return false;
		}
		SqlCommandType commandType = invocation.getMappedStatement().getSqlCommandType();
		return commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE
				|| commandType == SqlCommandType.DELETE;
	}

	@Override
	public FrameworkChangeMetadata resolve(DataChangeInvocation invocation) {
		TableMonitor monitor = invocation.getTableMonitor();
		JdbcTableMetadata jdbcMetadata = invocation.getJdbcTableMetadata();
		TableIdentifier table = monitor == null || monitor.getName() == null ? null
				: new TableIdentifier(monitor.getCatalog(), monitor.getSchema(), monitor.getName());
		if (table == null && jdbcMetadata != null) {
			table = jdbcMetadata.getTable();
		}
		List<String> keyColumns = monitor == null || monitor.getKeyColumns().isEmpty()
				? jdbcKeyColumns(jdbcMetadata) : monitor.getKeyColumns();
		return new FrameworkChangeMetadata(table, keyColumns, logicalDelete(monitor), GENERATED_KEY_ACCESSOR);
	}

	private List<String> jdbcKeyColumns(JdbcTableMetadata jdbcMetadata) {
		return jdbcMetadata == null ? Collections.<String>emptyList() : jdbcMetadata.getKeyColumns();
	}

	private LogicalDeleteMetadata logicalDelete(TableMonitor monitor) {
		if (monitor == null) {
			return null;
		}
		LogicalDeleteConfiguration configuration = monitor.getLogicalDelete();
		return configuration == null ? null : new LogicalDeleteMetadata(configuration.getColumn(),
				configuration.getDeletedValue(), configuration.getNotDeletedValue());
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
