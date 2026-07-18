package com.vesoft.nebula.jdbc.mysql.protocol;

public class MysqlErrPacket {
	private final int errorCode;
	private final String sqlState;
	private final String message;

	private MysqlErrPacket(int errorCode, String sqlState, String message) {
		this.errorCode = errorCode;
		this.sqlState = sqlState == null ? "HY000" : sqlState;
		this.message = message == null ? "" : message;
	}

	public static MysqlErrPacket of(int errorCode, String sqlState, String message) {
		return new MysqlErrPacket(errorCode, sqlState, message);
	}

	public byte[] toPayload() {
		return new MysqlPacketWriter()
				.writeByte(0xff)
				.writeIntLE(errorCode, 2)
				.writeByte('#')
				.writeString(sqlState)
				.writeString(message)
				.toByteArray();
	}
}
