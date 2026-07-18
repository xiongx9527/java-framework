package com.vesoft.nebula.jdbc.mysql.protocol;

import java.util.Arrays;

public class MysqlPacket {
	private final int sequenceId;
	private final byte[] payload;

	public MysqlPacket(int sequenceId, byte[] payload) {
		this.sequenceId = sequenceId & 0xff;
		this.payload = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
	}

	public static MysqlPacket fromBytes(byte[] packet) {
		if (packet == null || packet.length < 4) {
			throw new IllegalArgumentException("MySQL packet must contain a 4-byte header.");
		}
		int length = (packet[0] & 0xff) | ((packet[1] & 0xff) << 8) | ((packet[2] & 0xff) << 16);
		if (packet.length != length + 4) {
			throw new IllegalArgumentException("MySQL packet payload length does not match header.");
		}
		byte[] payload = new byte[length];
		System.arraycopy(packet, 4, payload, 0, length);
		return new MysqlPacket(packet[3] & 0xff, payload);
	}

	public byte[] toBytes() {
		byte[] packet = new byte[payload.length + 4];
		packet[0] = (byte) (payload.length & 0xff);
		packet[1] = (byte) ((payload.length >>> 8) & 0xff);
		packet[2] = (byte) ((payload.length >>> 16) & 0xff);
		packet[3] = (byte) sequenceId;
		System.arraycopy(payload, 0, packet, 4, payload.length);
		return packet;
	}

	public int getSequenceId() {
		return sequenceId;
	}

	public byte[] getPayload() {
		return Arrays.copyOf(payload, payload.length);
	}
}
