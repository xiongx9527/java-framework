package com.vesoft.nebula.jdbc.mysql.protocol;

public class MysqlPacketReader {
	private final byte[] payload;
	private int position;

	public MysqlPacketReader(byte[] payload) {
		this.payload = payload == null ? new byte[0] : payload;
	}

	public int readUnsignedByte() {
		ensureAvailable(1);
		return payload[position++] & 0xff;
	}

	public int readUnsignedShortLE() {
		int b0 = readUnsignedByte();
		int b1 = readUnsignedByte();
		return b0 | (b1 << 8);
	}

	public long readUnsignedIntLE() {
		long b0 = readUnsignedByte();
		long b1 = readUnsignedByte();
		long b2 = readUnsignedByte();
		long b3 = readUnsignedByte();
		return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
	}

	public long readLengthEncodedInteger() {
		int first = readUnsignedByte();
		if (first < 0xfb) {
			return first;
		}
		if (first == 0xfc) {
			return readUnsignedShortLE();
		}
		if (first == 0xfd) {
			long b0 = readUnsignedByte();
			long b1 = readUnsignedByte();
			long b2 = readUnsignedByte();
			return b0 | (b1 << 8) | (b2 << 16);
		}
		if (first == 0xfe) {
			long value = 0;
			for (int i = 0; i < 8; i++) {
				value |= ((long) readUnsignedByte()) << (8 * i);
			}
			return value;
		}
		return -1;
	}

	public String readNullTerminatedString() {
		int start = position;
		while (position < payload.length && payload[position] != 0) {
			position++;
		}
		String value = new String(payload, start, position - start, MysqlCharsets.UTF8);
		if (position < payload.length) {
			position++;
		}
		return value;
	}

	public String readRestString() {
		String value = new String(payload, position, payload.length - position, MysqlCharsets.UTF8);
		position = payload.length;
		return value;
	}

	public byte[] readBytes(int length) {
		ensureAvailable(length);
		byte[] value = new byte[length];
		System.arraycopy(payload, position, value, 0, length);
		position += length;
		return value;
	}

	public boolean hasRemaining() {
		return position < payload.length;
	}

	private void ensureAvailable(int length) {
		if (position + length > payload.length) {
			throw new IllegalArgumentException("MySQL packet payload ended unexpectedly.");
		}
	}
}
