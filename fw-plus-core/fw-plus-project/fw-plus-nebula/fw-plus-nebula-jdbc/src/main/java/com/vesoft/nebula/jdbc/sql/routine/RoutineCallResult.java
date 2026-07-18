package com.vesoft.nebula.jdbc.sql.routine;

import java.sql.ResultSet;

public class RoutineCallResult {

	private final ResultSet resultSet;
	private final int updateCount;

	private RoutineCallResult(ResultSet resultSet, int updateCount) {
		this.resultSet = resultSet;
		this.updateCount = updateCount;
	}

	public static RoutineCallResult resultSet(ResultSet resultSet) {
		return new RoutineCallResult(resultSet, 0);
	}

	public static RoutineCallResult updateCount(int updateCount) {
		return new RoutineCallResult(null, updateCount);
	}

	public boolean hasResultSet() {
		return resultSet != null;
	}

	public ResultSet getResultSet() {
		return resultSet;
	}

	public int getUpdateCount() {
		return updateCount;
	}
}
