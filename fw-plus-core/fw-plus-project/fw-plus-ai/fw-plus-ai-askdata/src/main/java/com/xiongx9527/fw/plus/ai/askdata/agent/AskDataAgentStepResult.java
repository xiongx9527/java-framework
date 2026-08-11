package com.xiongx9527.fw.plus.ai.askdata.agent;

import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 稳定步骤 Agent 的不可变类型化输出。
 */
public final class AskDataAgentStepResult {

	private final boolean success;
	private final List<String> selectedResources;
	private final String sql;
	private final AskDataSqlCommand sqlCommand;
	private final AskDataSqlResult sqlResult;
	private final String interpretation;
	private final String errorCode;
	private final String errorMessage;

	private AskDataAgentStepResult(boolean success, List<String> selectedResources, String sql,
			AskDataSqlCommand sqlCommand, AskDataSqlResult sqlResult, String interpretation,
			String errorCode, String errorMessage) {
		this.success = success;
		this.selectedResources = immutableResources(selectedResources);
		this.sql = normalize(sql);
		this.sqlCommand = sqlCommand;
		this.sqlResult = sqlResult;
		this.interpretation = normalize(interpretation);
		this.errorCode = normalize(errorCode);
		this.errorMessage = normalize(errorMessage);
	}

	public static AskDataAgentStepResult selectedTables(List<String> selectedResources) {
		return new AskDataAgentStepResult(true, selectedResources, null, null, null, null, null, null);
	}

	public static AskDataAgentStepResult generatedSql(String sql) {
		if (normalize(sql) == null) {
			throw new IllegalArgumentException("sql 不能为空白");
		}
		return new AskDataAgentStepResult(true, null, sql, null, null, null, null, null);
	}

	public static AskDataAgentStepResult validatedSql(AskDataSqlCommand sqlCommand) {
		if (sqlCommand == null) {
			throw new IllegalArgumentException("sqlCommand 不能为空");
		}
		return new AskDataAgentStepResult(true, null, null, sqlCommand, null, null, null, null);
	}

	public static AskDataAgentStepResult executedSql(AskDataSqlResult sqlResult) {
		if (sqlResult == null) {
			throw new IllegalArgumentException("sqlResult 不能为空");
		}
		return new AskDataAgentStepResult(true, null, null, null, sqlResult, null, null, null);
	}

	public static AskDataAgentStepResult interpreted(String interpretation) {
		return new AskDataAgentStepResult(true, null, null, null, null, interpretation, null, null);
	}

	public static AskDataAgentStepResult failure(String errorCode, String errorMessage) {
		if (normalize(errorMessage) == null) {
			throw new IllegalArgumentException("errorMessage 不能为空白");
		}
		return new AskDataAgentStepResult(false, null, null, null, null, null, errorCode, errorMessage);
	}

	public boolean isSuccess() {
		return success;
	}

	public List<String> getSelectedResources() {
		return selectedResources;
	}

	public String getSql() {
		return sql;
	}

	public AskDataSqlCommand getSqlCommand() {
		return sqlCommand;
	}

	public AskDataSqlResult getSqlResult() {
		return sqlResult;
	}

	public String getInterpretation() {
		return interpretation;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	private static List<String> immutableResources(List<String> resources) {
		if (resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> copy = new ArrayList<String>(resources.size());
		for (String resource : resources) {
			if (normalize(resource) == null) {
				throw new IllegalArgumentException("selectedResources 不能包含空白值");
			}
			copy.add(resource.trim());
		}
		return Collections.unmodifiableList(copy);
	}

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}
}
