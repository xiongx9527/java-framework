package com.vesoft.nebula.jdbc.mysql.security;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MysqlGatewaySecurityPolicyTest {

	@Test
	public void shouldAllowConfiguredOperationsOnly() {
		MysqlGatewaySecurityPolicy policy = MysqlGatewaySecurityPolicy.builder()
				.allowRead(true)
				.allowWrite(false)
				.allowDdl(false)
				.allowMetadata(true)
				.build();

		assertTrue(policy.isAllowed(session(false), MysqlGatewayOperation.READ));
		assertFalse(policy.isAllowed(session(false), MysqlGatewayOperation.WRITE));
		assertFalse(policy.isAllowed(session(false), MysqlGatewayOperation.DDL));
		assertTrue(policy.isAllowed(session(false), MysqlGatewayOperation.METADATA));
	}

	@Test
	public void shouldRequireSslWhenConfigured() {
		MysqlGatewaySecurityPolicy policy = MysqlGatewaySecurityPolicy.builder()
				.sslMode(MysqlSslMode.REQUIRED)
				.build();

		assertFalse(policy.isTransportAllowed(session(false)));
		assertTrue(policy.isTransportAllowed(session(true)));
	}

	private MysqlProtocolSession session(boolean ssl) {
		MysqlProtocolSession session = new MysqlProtocolSession(1L);
		session.authenticate("writer", "basketballplayer", 0);
		session.setSslEstablished(ssl);
		return session;
	}
}
