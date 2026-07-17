package com.vesoft.nebula.jdbc.mysql.protocol;

public enum MysqlEofPacket {
	INSTANCE;

	public byte[] toPayload() {
		return new MysqlPacketWriter()
				.writeByte(0xfe)
				.writeIntLE(0, 2)
				.writeIntLE(0x0002, 2)
				.toByteArray();
	}
}
