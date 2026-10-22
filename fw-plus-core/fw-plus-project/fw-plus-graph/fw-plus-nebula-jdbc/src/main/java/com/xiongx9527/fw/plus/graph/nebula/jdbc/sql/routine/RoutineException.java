package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class RoutineException extends BizException {

	/** 状态码 50009001（SERVER/DATABASE/001 nebula routine 执行错误） */
	public static final int CODE = 50_009_001;

	public static final int ER_PARSE_ERROR = 1064;
	public static final int ER_SP_ALREADY_EXISTS = 1304;
	public static final int ER_SP_DOES_NOT_EXIST = 1305;
	public static final int ER_SP_WRONG_NO_OF_ARGS = 1318;
	public static final int ER_NOT_SUPPORTED_YET = 1235;
	public static final int ER_QUERY_INTERRUPTED = 1317;
	public static final int ER_TABLEACCESS_DENIED_ERROR = 1142;

	private final String sqlState;
	private final int vendorCode;

	public RoutineException(String message, String sqlState, int vendorCode) {
		super(CODE, message);
		this.sqlState = sqlState;
		this.vendorCode = vendorCode;
	}

	public String getSQLState() {
		return sqlState;
	}

	public int getErrorCode() {
		return vendorCode;
	}

	public static RoutineException syntax(String message) {
		return new RoutineException(message, "42000", ER_PARSE_ERROR);
	}

	public static RoutineException unsupported(String feature) {
		return new RoutineException("网关 routine 子集不支持该能力: " + feature,
				"42000", ER_NOT_SUPPORTED_YET);
	}

	public static RoutineException alreadyExists(String name) {
		return new RoutineException("Routine already exists: " + name,
				"42000", ER_SP_ALREADY_EXISTS);
	}

	public static RoutineException notFound(String name) {
		return new RoutineException("Routine does not exist: " + name,
				"42000", ER_SP_DOES_NOT_EXIST);
	}

	public static RoutineException invalidArguments(String name) {
		return new RoutineException("Invalid routine arguments: " + name,
				"42000", ER_SP_WRONG_NO_OF_ARGS);
	}

	public static RoutineException executionLimit(String message) {
		return new RoutineException(message, "70100", ER_QUERY_INTERRUPTED);
	}

	public static RoutineException permissionDenied(RoutinePermissionChecker.Action action, String name) {
		return new RoutineException("Routine permission denied: " + action + " " + name,
				"42000", ER_TABLEACCESS_DENIED_ERROR);
	}
}
