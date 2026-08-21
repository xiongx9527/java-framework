package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryRoutineMetadataRepository implements RoutineMetadataRepository {

	private final ConcurrentMap<String, RoutineDefinition> definitions =
			new ConcurrentHashMap<String, RoutineDefinition>();

	@Override
	public RoutineDefinition find(RoutineName name) throws SQLException {
		return definitions.get(name.key());
	}

	@Override
	public void save(RoutineDefinition definition) throws SQLException {
		definitions.put(definition.routineName().key(), definition);
	}

	@Override
	public boolean drop(RoutineName name) throws SQLException {
		return definitions.remove(name.key()) != null;
	}

	public void clear() {
		definitions.clear();
	}
}
