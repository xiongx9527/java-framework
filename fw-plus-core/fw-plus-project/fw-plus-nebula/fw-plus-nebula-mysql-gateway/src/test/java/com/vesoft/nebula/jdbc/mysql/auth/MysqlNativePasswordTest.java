package com.vesoft.nebula.jdbc.mysql.auth;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MysqlNativePasswordTest {
	@Test
	public void shouldValidateMysqlNativePasswordScramble() {
		byte[] seed = "12345678901234567890".getBytes();
		byte[] response = MysqlNativePassword.scramble("secret", seed);

		assertTrue(MysqlNativePassword.matches("secret", seed, response));
		assertFalse(MysqlNativePassword.matches("wrong", seed, response));
	}

	@Test
	public void shouldRejectWrongPasswordOrDatabase() {
		StaticMysqlGatewayAuthenticator authenticator = new StaticMysqlGatewayAuthenticator(
				Collections.singletonList(new MysqlGatewayUser("reader", "secret", Collections.singletonList("basketballplayer")))
		);

		assertTrue(authenticator.authenticatePlain("reader", "secret", "basketballplayer").isAuthenticated());
		assertFalse(authenticator.authenticatePlain("reader", "bad", "basketballplayer").isAuthenticated());
		assertFalse(authenticator.authenticatePlain("reader", "secret", "other").isAuthenticated());
	}
}
