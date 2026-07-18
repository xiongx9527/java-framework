package com.vesoft.nebula.jdbc.mysql.protocol;

import com.vesoft.nebula.jdbc.mysql.auth.MysqlAuthenticationResult;
import com.vesoft.nebula.jdbc.mysql.auth.MysqlGatewayAuthenticator;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

public class MysqlHandshakeHandler {
	private final MysqlGatewayAuthenticator authenticator;
	private final byte[] seed;

	public MysqlHandshakeHandler(MysqlGatewayAuthenticator authenticator, byte[] seed) {
		this.authenticator = authenticator;
		this.seed = seed;
	}

	public MysqlAuthenticationResult authenticate(MysqlProtocolSession session, byte[] responsePayload) {
		MysqlHandshakeResponse response = MysqlHandshakeResponse.parse(responsePayload);
		MysqlAuthenticationResult result = authenticator.authenticateNative(
				response.getUsername(),
				seed,
				response.getAuthResponse(),
				response.getDatabase()
		);
		if (result.isAuthenticated()) {
			session.authenticate(response.getUsername(), response.getDatabase(), response.getCapabilityFlags());
		}
		return result;
	}
}
