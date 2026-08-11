package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadata;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

public final class DataChangeInvocation {
	private final MappedStatement mappedStatement;
	private final BoundSql boundSql;
	private final Object parameterObject;
	private final TableMonitor tableMonitor;
	private final JdbcTableMetadata jdbcTableMetadata;

	public DataChangeInvocation(MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject) {
		this(mappedStatement, boundSql, parameterObject, null, null);
	}

	public DataChangeInvocation(MappedStatement mappedStatement, BoundSql boundSql, Object parameterObject,
			TableMonitor tableMonitor, JdbcTableMetadata jdbcTableMetadata) {
		this.mappedStatement = mappedStatement;
		this.boundSql = boundSql;
		this.parameterObject = parameterObject;
		this.tableMonitor = tableMonitor;
		this.jdbcTableMetadata = jdbcTableMetadata;
	}

	public MappedStatement getMappedStatement() { return mappedStatement; }
	public BoundSql getBoundSql() { return boundSql; }
	public Object getParameterObject() { return parameterObject; }
	public TableMonitor getTableMonitor() { return tableMonitor; }
	public JdbcTableMetadata getJdbcTableMetadata() { return jdbcTableMetadata; }
}
