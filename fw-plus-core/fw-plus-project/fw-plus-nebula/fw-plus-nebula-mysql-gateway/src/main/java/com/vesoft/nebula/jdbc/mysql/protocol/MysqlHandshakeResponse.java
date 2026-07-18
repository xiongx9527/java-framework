package com.vesoft.nebula.jdbc.mysql.protocol;

public class MysqlHandshakeResponse {
	private final int capabilityFlags;
	private final String username;
	private final byte[] authResponse;
	private final String database;

	private MysqlHandshakeResponse(int capabilityFlags, String username, byte[] authResponse, String database) {
		this.capabilityFlags = capabilityFlags;
		this.username = username;
		this.authResponse = authResponse;
		this.database = database;
	}

	public static MysqlHandshakeResponse parse(byte[] payload) {
		MysqlPacketReader reader = new MysqlPacketReader(payload);
		int capabilities = (int) reader.readUnsignedIntLE();
		reader.readUnsignedIntLE();
		reader.readUnsignedByte();
		reader.readBytes(23);
		String username = reader.readNullTerminatedString();
		int authLength = reader.readUnsignedByte();
		byte[] authResponse = reader.readBytes(authLength);
		String database = null;
		if ((capabilities & MysqlCapability.CLIENT_CONNECT_WITH_DB) != 0 && reader.hasRemaining()) {
			database = reader.readNullTerminatedString();
		}
		return new MysqlHandshakeResponse(capabilities, username, authResponse, database);
	}

	public boolean hasCapability(int capability) {
		return (capabilityFlags & capability) != 0;
	}

	public int getCapabilityFlags() {
		return capabilityFlags;
	}

	public String getUsername() {
		return username;
	}

	public byte[] getAuthResponse() {
		byte[] copy = new byte[authResponse.length];
		System.arraycopy(authResponse, 0, copy, 0, authResponse.length);
		return copy;
	}

	public String getDatabase() {
		return database;
	}
}
