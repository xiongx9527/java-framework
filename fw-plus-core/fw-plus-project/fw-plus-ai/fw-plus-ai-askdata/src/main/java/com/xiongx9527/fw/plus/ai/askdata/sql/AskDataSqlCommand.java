package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AskData SQL 管线的不可变命令。
 *
 * <p>命令携带项目和操作人隔离上下文、命名参数与获授权对象。它不包含 JDBC 连接、数据源或执行器，
 * 因而 SQL 插件只能改写命令，不能绕过最终安全校验直接访问数据库。</p>
 */
public final class AskDataSqlCommand {

	private final AskDataProjectContext context;
	private final String sql;
	private final List<AskDataSqlParameter> parameters;
	private final Map<String, AskDataSqlParameter> parametersByName;
	private final Set<String> authorizedObjects;

	public AskDataSqlCommand(AskDataProjectContext context, String sql, List<AskDataSqlParameter> parameters,
			Set<String> authorizedObjects) {
		if (context == null) {
			throw new IllegalArgumentException("context 不能为空");
		}
		if (sql == null || sql.trim().length() == 0) {
			throw new IllegalArgumentException("sql 不能为空白");
		}
		if (parameters == null || authorizedObjects == null) {
			throw new IllegalArgumentException("parameters 和 authorizedObjects 不能为空");
		}
		this.context = context;
		this.sql = sql.trim();
		this.parameters = copyParameters(parameters);
		this.parametersByName = indexParameters(this.parameters);
		this.authorizedObjects = copyAuthorizedObjects(authorizedObjects);
	}

	public AskDataProjectContext getContext() {
		return context;
	}

	public String getSql() {
		return sql;
	}

	public List<AskDataSqlParameter> getParameters() {
		return parameters;
	}

	public AskDataSqlParameter getParameter(String name) {
		return parametersByName.get(name);
	}

	public Set<String> getAuthorizedObjects() {
		return authorizedObjects;
	}

	/**
	 * 返回仅替换 SQL 文本的新命令，保留隔离上下文、参数和授权对象。
	 *
	 * <p>插件改写后的命令仍会由插件链进行最终安全校验。</p>
	 *
	 * @param rewrittenSql 插件改写后的 SQL
	 * @return 新命令
	 */
	public AskDataSqlCommand withSql(String rewrittenSql) {
		return new AskDataSqlCommand(context, rewrittenSql, parameters, authorizedObjects);
	}

	private static List<AskDataSqlParameter> copyParameters(List<AskDataSqlParameter> source) {
		List<AskDataSqlParameter> copy = new ArrayList<AskDataSqlParameter>(source.size());
		for (AskDataSqlParameter parameter : source) {
			if (parameter == null) {
				throw new IllegalArgumentException("parameters 不能包含 null");
			}
			copy.add(parameter);
		}
		return Collections.unmodifiableList(copy);
	}

	private static Map<String, AskDataSqlParameter> indexParameters(List<AskDataSqlParameter> source) {
		Map<String, AskDataSqlParameter> index = new LinkedHashMap<String, AskDataSqlParameter>();
		for (AskDataSqlParameter parameter : source) {
			if (index.put(parameter.getName(), parameter) != null) {
				throw new IllegalArgumentException("命名参数重复: " + parameter.getName());
			}
		}
		return Collections.unmodifiableMap(index);
	}

	private static Set<String> copyAuthorizedObjects(Set<String> source) {
		Set<String> copy = new LinkedHashSet<String>();
		for (String object : source) {
			if (object == null || object.trim().length() == 0) {
				throw new IllegalArgumentException("authorizedObjects 不能包含空白对象名");
			}
			copy.add(object.trim());
		}
		return Collections.unmodifiableSet(copy);
	}
}
