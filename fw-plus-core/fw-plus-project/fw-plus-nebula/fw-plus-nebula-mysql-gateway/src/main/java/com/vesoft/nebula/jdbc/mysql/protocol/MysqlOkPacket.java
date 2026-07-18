package com.vesoft.nebula.jdbc.mysql.protocol;

public class MysqlOkPacket {
	private final long affectedRows;
	private final int statusFlags;
	private final int warnings;

	private MysqlOkPacket(long affectedRows, int statusFlags, int warnings) {
		this.affectedRows = affectedRows;
		this.statusFlags = statusFlags;
		this.warnings = warnings;
	}

	public static MysqlOkPacket success() {
		return success(0, 0);
	}

	public static MysqlOkPacket success(long affectedRows, int warnings) {
		return new MysqlOkPacket(affectedRows, 0x0002, warnings);
	}

	public byte[] toPayload() {
		return new MysqlPacketWriter()
				.writeByte(0x00)
				.writeLengthEncodedInteger(affectedRows)
				.writeLengthEncodedInteger(0)
				.writeIntLE(statusFlags, 2)
				.writeIntLE(warnings, 2)
				.toByteArray();
	}
}
