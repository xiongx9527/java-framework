package com.vesoft.nebula.jdbc.mysql.auth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MysqlGatewayUser {
	private final String username;
	private final String password;
	private final List<String> allowedDatabases;

	public MysqlGatewayUser(String username, String password, List<String> allowedDatabases) {
		this.username = username;
		this.password = password;
		this.allowedDatabases = allowedDatabases == null
				? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(allowedDatabases));
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public List<String> getAllowedDatabases() {
		return allowedDatabases;
	}

	public boolean allowsDatabase(String database) {
		return database == null || database.length() == 0 || allowedDatabases.contains(database);
	}
}
