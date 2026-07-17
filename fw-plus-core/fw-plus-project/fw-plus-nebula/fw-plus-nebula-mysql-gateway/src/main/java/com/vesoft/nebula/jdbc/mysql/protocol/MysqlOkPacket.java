package com.vesoft.nebula.jdbc.mysql.protocol;

public class MysqlOkPacket {
	private final int statusFlags;
	private final int warnings;

	private MysqlOkPacket(int statusFlags, int warnings) {
		this.statusFlags = statusFlags;
		this.warnings = warnings;
	}

	public static MysqlOkPacket success() {
		return new MysqlOkPacket(0x0002, 0);
	}

	public byte[] toPayload() {
		return new MysqlPacketWriter()
				.writeByte(0x00)
				.writeLengthEncodedInteger(0)
				.writeLengthEncodedInteger(0)
				.writeIntLE(statusFlags, 2)
				.writeIntLE(warnings, 2)
				.toByteArray();
	}
}
