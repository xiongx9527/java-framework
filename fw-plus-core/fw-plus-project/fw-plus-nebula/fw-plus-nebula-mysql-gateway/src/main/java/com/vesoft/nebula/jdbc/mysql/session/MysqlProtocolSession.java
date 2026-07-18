package com.vesoft.nebula.jdbc.mysql.session;

import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewaySessionContext;

import java.util.Collections;

public class MysqlProtocolSession {
	private final long connectionId;
	private String username;
	private String database;
	private int clientCapabilities;
	private boolean authenticated;
	private boolean closed;

	public MysqlProtocolSession(long connectionId) {
		this.connectionId = connectionId;
	}

	public void authenticate(String username, String database, int clientCapabilities) {
		this.username = username;
		this.database = database;
		this.clientCapabilities = clientCapabilities;
		this.authenticated = true;
	}

	public MysqlGatewaySessionContext toGatewayContext() {
		return new MysqlGatewaySessionContext(connectionId, username, database, Collections.singletonList(database));
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

	public void setDatabase(String database) {
		this.database = database;
	}

	public int getClientCapabilities() {
		return clientCapabilities;
	}

	public boolean isAuthenticated() {
		return authenticated;
	}

	public boolean isClosed() {
		return closed;
	}

	public void close() {
		closed = true;
	}
}
