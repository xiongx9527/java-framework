package com.vesoft.nebula.jdbc.sql.routine;

import java.sql.SQLException;

public interface RoutineMetadataRepository {

	RoutineDefinition find(RoutineName name) throws SQLException;

	void save(RoutineDefinition definition) throws SQLException;

	boolean drop(RoutineName name) throws SQLException;
}
