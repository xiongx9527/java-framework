package com.vesoft.nebula.jdbc.mysql.session;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MysqlProtocolSessionTest {
	@Test
	public void shouldTrackAuthenticationAndDatabase() {
		MysqlProtocolSession session = new MysqlProtocolSession(9L);
		session.authenticate("reader", "basketballplayer", 0x00000200);

		assertEquals(9L, session.getConnectionId());
		assertEquals("reader", session.getUsername());
		assertEquals("basketballplayer", session.getDatabase());
		assertTrue(session.isAuthenticated());
	}
}
