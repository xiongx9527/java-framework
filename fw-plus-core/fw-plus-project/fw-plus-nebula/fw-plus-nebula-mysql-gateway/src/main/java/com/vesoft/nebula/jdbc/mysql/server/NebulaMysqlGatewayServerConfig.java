package com.vesoft.nebula.jdbc.mysql.server;

import com.vesoft.nebula.jdbc.mysql.auth.MysqlGatewayAuthenticator;
import com.vesoft.nebula.jdbc.mysql.auth.MysqlGatewayUser;
import com.vesoft.nebula.jdbc.mysql.auth.StaticMysqlGatewayAuthenticator;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCharsets;
import com.vesoft.nebula.jdbc.mysql.query.MysqlColumn;
import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewaySessionContext;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.NebulaSqlExecutionPort;
import com.vesoft.nebula.jdbc.mysql.query.ReadonlyQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.StaticMysqlMetadataHandler;
import com.vesoft.nebula.jdbc.mysql.security.MysqlGatewaySecurityPolicy;
import com.vesoft.nebula.jdbc.mysql.security.MysqlSslMode;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;

public class NebulaMysqlGatewayServerConfig {
	private static final String DEFAULT_SERVER_VERSION = "5.7.0-fw-nebula-gateway";
	private static final byte[] DEFAULT_SEED = "12345678901234567890".getBytes(MysqlCharsets.UTF8);
	private final String host;
	private final int port;
	private final String serverVersion;
	private final byte[] seed;
	private final MysqlGatewayAuthenticator authenticator;
	private final ReadonlyQueryHandler queryHandler;
	private final MysqlGatewaySecurityPolicy securityPolicy;

	public NebulaMysqlGatewayServerConfig(String host, int port) {
		this(host, port, DEFAULT_SERVER_VERSION, DEFAULT_SEED, defaultAuthenticator(), defaultQueryHandler(),
				MysqlGatewaySecurityPolicy.allowAll());
	}

	public NebulaMysqlGatewayServerConfig(String host, int port, MysqlGatewaySecurityPolicy securityPolicy) {
		this(host, port, DEFAULT_SERVER_VERSION, DEFAULT_SEED, defaultAuthenticator(), defaultQueryHandler(),
				securityPolicy);
	}

	public NebulaMysqlGatewayServerConfig(String host, int port, MysqlGatewayAuthenticator authenticator,
										  ReadonlyQueryHandler queryHandler) {
		this(host, port, DEFAULT_SERVER_VERSION, DEFAULT_SEED, authenticator, queryHandler,
				MysqlGatewaySecurityPolicy.allowAll());
	}

	public NebulaMysqlGatewayServerConfig(String host, int port, MysqlGatewayAuthenticator authenticator,
										  ReadonlyQueryHandler queryHandler,
										  MysqlGatewaySecurityPolicy securityPolicy) {
		this(host, port, DEFAULT_SERVER_VERSION, DEFAULT_SEED, authenticator, queryHandler, securityPolicy);
	}

	public NebulaMysqlGatewayServerConfig(String host, int port, String serverVersion, byte[] seed,
										  MysqlGatewayAuthenticator authenticator, ReadonlyQueryHandler queryHandler) {
		this(host, port, serverVersion, seed, authenticator, queryHandler, MysqlGatewaySecurityPolicy.allowAll());
	}

	public NebulaMysqlGatewayServerConfig(String host, int port, String serverVersion, byte[] seed,
										  MysqlGatewayAuthenticator authenticator, ReadonlyQueryHandler queryHandler,
										  MysqlGatewaySecurityPolicy securityPolicy) {
		this.host = host == null ? "0.0.0.0" : host;
		this.port = port;
		this.serverVersion = serverVersion == null ? DEFAULT_SERVER_VERSION : serverVersion;
		this.seed = copySeed(seed == null ? DEFAULT_SEED : seed);
		this.authenticator = authenticator == null ? defaultAuthenticator() : authenticator;
		this.queryHandler = queryHandler == null ? defaultQueryHandler() : queryHandler;
		this.securityPolicy = securityPolicy == null ? MysqlGatewaySecurityPolicy.allowAll() : securityPolicy;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	public byte[] getSeed() {
		return copySeed(seed);
	}

	public MysqlGatewayAuthenticator getAuthenticator() {
		return authenticator;
	}

	public ReadonlyQueryHandler getQueryHandler() {
		return queryHandler;
	}

	public MysqlGatewaySecurityPolicy getSecurityPolicy() {
		return securityPolicy;
	}

	public MysqlSslMode getSslMode() {
		return securityPolicy.getSslMode();
	}

	private static MysqlGatewayAuthenticator defaultAuthenticator() {
		return new StaticMysqlGatewayAuthenticator(Collections.singletonList(
				new MysqlGatewayUser("root", "", Collections.<String>emptyList())
		));
	}

	private static ReadonlyQueryHandler defaultQueryHandler() {
		return new ReadonlyQueryHandler(new SelectOneExecutionPort(), new StaticMysqlMetadataHandler("nebula"));
	}

	private static byte[] copySeed(byte[] seed) {
		byte[] copy = new byte[seed.length];
		System.arraycopy(seed, 0, copy, 0, seed.length);
		return copy;
	}

	private static class SelectOneExecutionPort implements NebulaSqlExecutionPort {
		@Override
		public MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) throws SQLException {
			String normalized = sql == null ? "" : sql.trim();
			if (normalized.endsWith(";")) {
				normalized = normalized.substring(0, normalized.length() - 1).trim();
			}
			if ("SELECT 1".equalsIgnoreCase(normalized)) {
				return MysqlQueryResult.of(
						Collections.singletonList(new MysqlColumn("1", "1", Types.INTEGER, false, true)),
						Collections.<java.util.List<Object>>singletonList(Collections.<Object>singletonList(Integer.valueOf(1)))
				);
			}
			throw new SQLException("No Nebula query executor configured for SQL: " + normalized);
		}
	}
}
