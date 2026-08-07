package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BoundSqlContext {
	private final BoundSql boundSql;
	private final boolean lockRows;
	private final int defaultMaxRows;

	public BoundSqlContext(BoundSql boundSql) {
		this(boundSql, false, 1000);
	}

	public BoundSqlContext(BoundSql boundSql, boolean lockRows, int defaultMaxRows) {
		if (boundSql == null) {
			throw new IllegalArgumentException("boundSql must not be null");
		}
		if (defaultMaxRows <= 0) {
			throw new IllegalArgumentException("defaultMaxRows must be greater than zero");
		}
		this.boundSql = boundSql;
		this.lockRows = lockRows;
		this.defaultMaxRows = defaultMaxRows;
	}

	public BoundSql getBoundSql() { return boundSql; }
	public String getSql() { return boundSql.getSql(); }
	public Object getParameterObject() { return boundSql.getParameterObject(); }
	public List<ParameterMapping> getParameterMappings() {
		return Collections.unmodifiableList(new ArrayList<ParameterMapping>(boundSql.getParameterMappings()));
	}
	public boolean isLockRows() { return lockRows; }
	public int getDefaultMaxRows() { return defaultMaxRows; }
}
