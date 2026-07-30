package com.xiongx9527.fw.plus.ai.askdata.security;

import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 作用于已授权对象的参数化行级过滤条件。
 */
public final class AskDataRowPredicate {

	private final AskDataAuthorizedObject authorizedObject;
	private final String sqlFragment;
	private final List<AskDataSqlParameter> parameters;

	public AskDataRowPredicate(AskDataAuthorizedObject authorizedObject, String sqlFragment,
							   List<AskDataSqlParameter> parameters) {
		if (authorizedObject == null) {
			throw new IllegalArgumentException("authorizedObject 不能为空");
		}
		if (sqlFragment == null || sqlFragment.trim().length() == 0) {
			throw new IllegalArgumentException("sqlFragment 不能为空白");
		}
		if (parameters == null) {
			throw new IllegalArgumentException("parameters 不能为空");
		}
		this.authorizedObject = authorizedObject;
		this.sqlFragment = sqlFragment.trim();
		this.parameters = Collections.unmodifiableList(new ArrayList<AskDataSqlParameter>(parameters));
	}

	public AskDataAuthorizedObject getAuthorizedObject() {
		return authorizedObject;
	}

	public String getSqlFragment() {
		return sqlFragment;
	}

	public List<AskDataSqlParameter> getParameters() {
		return parameters;
	}
}
