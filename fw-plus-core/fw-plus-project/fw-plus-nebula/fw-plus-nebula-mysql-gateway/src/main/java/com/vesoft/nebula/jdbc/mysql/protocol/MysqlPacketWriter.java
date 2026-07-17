package com.vesoft.nebula.jdbc.mysql.protocol;

import java.io.ByteArrayOutputStream;

public class MysqlPacketWriter {
	private final ByteArrayOutputStream output = new ByteArrayOutputStream();

	public MysqlPacketWriter writeByte(int value) {
		output.write(value & 0xff);
		return this;
	}

	public MysqlPacketWriter writeBytes(byte[] value) {
		if (value != null) {
			output.write(value, 0, value.length);
		}
		return this;
	}

	public MysqlPacketWriter writeIntLE(long value, int size) {
		for (int i = 0; i < size; i++) {
			writeByte((int) (value >>> (8 * i)));
		}
		return this;
	}

	public MysqlPacketWriter writeLengthEncodedInteger(long value) {
		if (value < 0xfb) {
			return writeByte((int) value);
		}
		if (value <= 0xffffL) {
			return writeByte(0xfc).writeIntLE(value, 2);
		}
		if (value <= 0xffffffL) {
			return writeByte(0xfd).writeIntLE(value, 3);
		}
		return writeByte(0xfe).writeIntLE(value, 8);
	}

	public MysqlPacketWriter writeNullTerminatedString(String value) {
		writeString(value);
		writeByte(0);
		return this;
	}

	public MysqlPacketWriter writeLengthEncodedString(String value) {
		byte[] bytes = value == null ? new byte[0] : value.getBytes(MysqlCharsets.UTF8);
		writeLengthEncodedInteger(bytes.length);
		writeBytes(bytes);
		return this;
	}

	public MysqlPacketWriter writeString(String value) {
		if (value != null) {
			writeBytes(value.getBytes(MysqlCharsets.UTF8));
		}
		return this;
	}

	public byte[] toByteArray() {
		return output.toByteArray();
	}
}
