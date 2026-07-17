package com.vesoft.nebula.jdbc.mysql.server;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NebulaMysqlGatewayServerTest {
	@Test
	public void shouldStartAndStopOnEphemeralPort() throws Exception {
		NebulaMysqlGatewayServer server = new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0));

		server.start();
		try {
			assertTrue(server.isRunning());
			assertTrue(server.getBoundPort() > 0);
		} finally {
			server.stop();
		}

		assertFalse(server.isRunning());
	}
}
