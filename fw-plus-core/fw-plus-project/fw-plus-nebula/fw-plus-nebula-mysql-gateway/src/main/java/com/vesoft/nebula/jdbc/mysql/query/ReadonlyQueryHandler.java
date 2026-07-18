package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;
import java.util.Locale;

public class ReadonlyQueryHandler {
	private final NebulaSqlExecutionPort executionPort;
	private final MysqlMetadataHandler metadataHandler;
	private final ReadonlySqlGuard guard;

	public ReadonlyQueryHandler(NebulaSqlExecutionPort executionPort, MysqlMetadataHandler metadataHandler) {
		this.executionPort = executionPort;
		this.metadataHandler = metadataHandler;
		this.guard = new ReadonlySqlGuard();
	}

	public MysqlQueryResult handle(MysqlProtocolSession session, String sql) throws SQLException {
		guard.check(sql);
		String normalized = sql == null ? "" : sql.trim();
		if (normalized.length() == 0) {
			return MysqlQueryResult.of(java.util.Collections.<MysqlColumn>emptyList(), java.util.Collections.<java.util.List<Object>>emptyList());
		}
		String upper = normalized.toUpperCase(Locale.ENGLISH);
		if (upper.equals("SHOW DATABASES")) {
			return metadataHandler.showDatabases(session);
		}
		if (upper.equals("SHOW TABLES")) {
			return metadataHandler.showTables(session);
		}
		if (upper.startsWith("DESCRIBE ") || upper.startsWith("DESC ")) {
			return metadataHandler.describe(session, tableName(normalized));
		}
		try {
			return executionPort.executeQuery(session.toGatewayContext(), sql);
		} catch (SQLException exception) {
			throw MysqlErrorMapper.execution(exception);
		}
	}

	private String tableName(String sql) {
		String[] parts = sql.trim().split("\\s+");
		return parts.length < 2 ? "" : parts[1].replace("`", "");
	}
}
