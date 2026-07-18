package com.vesoft.nebula.jdbc.mysql.protocol;

import java.util.Arrays;

public class MysqlHandshakePacket {
	private static final int DEFAULT_CHARACTER_SET = 33;
	private static final int SERVER_STATUS_AUTOCOMMIT = 0x0002;
	private final String serverVersion;
	private final long connectionId;
	private final byte[] seed;
	private final int capabilityFlags;
	private final String authPluginName;

	public MysqlHandshakePacket(String serverVersion, long connectionId, byte[] seed, int capabilityFlags, String authPluginName) {
		this.serverVersion = serverVersion;
		this.connectionId = connectionId;
		this.seed = seed == null ? new byte[0] : Arrays.copyOf(seed, seed.length);
		this.capabilityFlags = capabilityFlags;
		this.authPluginName = authPluginName;
	}

	public byte[] toPayload() {
		byte[] firstSeed = Arrays.copyOf(seed, Math.min(8, seed.length));
		byte[] restSeed = seed.length <= 8 ? new byte[0] : Arrays.copyOfRange(seed, 8, seed.length);
		MysqlPacketWriter writer = new MysqlPacketWriter()
				.writeByte(10)
				.writeNullTerminatedString(serverVersion)
				.writeIntLE(connectionId, 4)
				.writeBytes(firstSeed)
				.writeByte(0)
				.writeIntLE(capabilityFlags & 0xffff, 2)
				.writeByte(DEFAULT_CHARACTER_SET)
				.writeIntLE(SERVER_STATUS_AUTOCOMMIT, 2)
				.writeIntLE((capabilityFlags >>> 16) & 0xffff, 2)
				.writeByte(seed.length + 1);
		writer.writeBytes(new byte[10]);
		writer.writeBytes(restSeed).writeByte(0);
		writer.writeNullTerminatedString(authPluginName);
		return writer.toByteArray();
	}
}
