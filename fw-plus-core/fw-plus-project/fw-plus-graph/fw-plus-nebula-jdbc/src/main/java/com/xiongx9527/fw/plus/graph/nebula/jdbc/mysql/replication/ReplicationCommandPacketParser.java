package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import java.nio.charset.StandardCharsets;

public class ReplicationCommandPacketParser {
	private static final int BINLOG_DUMP_MIN_LENGTH = 11;
	private static final int REGISTER_SLAVE_MIN_LENGTH = 6;

	public ReplicationCommandRequest parse(byte[] packet) throws ReplicationException {
		if (packet == null || packet.length == 0) {
			throw new ReplicationException(ReplicationErrorMapper.malformedPacket());
		}
		int commandCode = packet[0] & 0xff;
		ReplicationCommandType type = ReplicationCommandType.fromCommandCode(commandCode);
		if (type == ReplicationCommandType.COM_BINLOG_DUMP) {
			return parseBinlogDump(packet);
		}
		if (type == ReplicationCommandType.COM_REGISTER_SLAVE) {
			return parseRegisterSlave(packet);
		}
		if (type == ReplicationCommandType.COM_BINLOG_DUMP_GTID) {
			return ReplicationCommandRequest.unsupported(type);
		}
		return ReplicationCommandRequest.unknown(commandCode);
	}

	private ReplicationCommandRequest parseBinlogDump(byte[] packet) throws ReplicationException {
		if (packet.length < BINLOG_DUMP_MIN_LENGTH) {
			throw new ReplicationException(ReplicationErrorMapper.malformedPacket());
		}
		long position = littleEndianUnsignedInt(packet, 1);
		int flags = littleEndianUnsignedShort(packet, 5);
		long serverId = littleEndianUnsignedInt(packet, 7);
		String fileName = new String(packet, 11, packet.length - 11, StandardCharsets.UTF_8);
		return ReplicationCommandRequest.binlogDump(fileName, position, flags, serverId);
	}

	private ReplicationCommandRequest parseRegisterSlave(byte[] packet) throws ReplicationException {
		if (packet.length < REGISTER_SLAVE_MIN_LENGTH) {
			throw new ReplicationException(ReplicationErrorMapper.malformedPacket());
		}
		long serverId = littleEndianUnsignedInt(packet, 1);
		int hostLength = packet[5] & 0xff;
		if (packet.length < 6 + hostLength) {
			throw new ReplicationException(ReplicationErrorMapper.malformedPacket());
		}
		String hostName = new String(packet, 6, hostLength, StandardCharsets.UTF_8);
		return ReplicationCommandRequest.registerSlave(serverId, hostName);
	}

	private long littleEndianUnsignedInt(byte[] packet, int offset) {
		return ((long) packet[offset] & 0xff)
				| (((long) packet[offset + 1] & 0xff) << 8)
				| (((long) packet[offset + 2] & 0xff) << 16)
				| (((long) packet[offset + 3] & 0xff) << 24);
	}

	private int littleEndianUnsignedShort(byte[] packet, int offset) {
		return (packet[offset] & 0xff) | ((packet[offset + 1] & 0xff) << 8);
	}
}
