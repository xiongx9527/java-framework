package com.vesoft.nebula.jdbc.mysql.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MysqlGatewaySessionContext {
	private final long connectionId;
	private final String username;
	private final String database;
	private final List<String> allowedDatabases;

	public MysqlGatewaySessionContext(long connectionId, String username, String database, List<String> allowedDatabases) {
		this.connectionId = connectionId;
		this.username = username;
		this.database = database;
		this.allowedDatabases = allowedDatabases == null
				? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(allowedDatabases));
	}

	public long getConnectionId() {
		return connectionId;
	}

	public String getUsername() {
		return username;
	}

	public String getDatabase() {
		return database;
	}

	public List<String> getAllowedDatabases() {
		return allowedDatabases;
	}
}
