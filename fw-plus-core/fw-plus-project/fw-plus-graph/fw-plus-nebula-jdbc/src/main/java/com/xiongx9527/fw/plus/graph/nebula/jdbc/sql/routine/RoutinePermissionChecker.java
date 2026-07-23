package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import java.sql.SQLException;

public interface RoutinePermissionChecker {

	enum Action {
		MANAGE,
		EXECUTE
	}

	void check(String schema, String user, String routineName, Action action) throws SQLException;

	static RoutinePermissionChecker allowAll() {
		return new RoutinePermissionChecker() {
			@Override
			public void check(String schema, String user, String routineName, Action action) {
			}
		};
	}

	static RoutinePermissionChecker denyAll() {
		return new RoutinePermissionChecker() {
			@Override
			public void check(String schema, String user, String routineName, Action action) throws SQLException {
				throw RoutineException.permissionDenied(action, routineName);
			}
		};
	}
}
