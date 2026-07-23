package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public class ReplicationCommandRequest {
	private final ReplicationCommandType type;
	private final int rawCommandCode;
	private final long serverId;
	private final String hostName;
	private final String fileName;
	private final long position;
	private final int flags;

	private ReplicationCommandRequest(ReplicationCommandType type, int rawCommandCode, long serverId, String hostName,
			String fileName, long position, int flags) {
		this.type = type;
		this.rawCommandCode = rawCommandCode;
		this.serverId = serverId;
		this.hostName = hostName;
		this.fileName = fileName;
		this.position = position;
		this.flags = flags;
	}

	public static ReplicationCommandRequest registerSlave(long serverId, String hostName) {
		return new ReplicationCommandRequest(ReplicationCommandType.COM_REGISTER_SLAVE,
				ReplicationCommandType.COM_REGISTER_SLAVE.getCommandCode(), serverId, hostName, null, 0L, 0);
	}

	public static ReplicationCommandRequest binlogDump(String fileName, long position, int flags, long serverId) {
		return new ReplicationCommandRequest(ReplicationCommandType.COM_BINLOG_DUMP,
				ReplicationCommandType.COM_BINLOG_DUMP.getCommandCode(), serverId, null, fileName, position, flags);
	}

	public static ReplicationCommandRequest unsupported(ReplicationCommandType type) {
		return new ReplicationCommandRequest(type, type.getCommandCode(), 0L, null, null, 0L, 0);
	}

	public static ReplicationCommandRequest unknown(int rawCommandCode) {
		return new ReplicationCommandRequest(ReplicationCommandType.UNKNOWN, rawCommandCode, 0L, null, null, 0L, 0);
	}

	public ReplicationCommandType getType() {
		return type;
	}

	public int getRawCommandCode() {
		return rawCommandCode;
	}

	public long getServerId() {
		return serverId;
	}

	public String getHostName() {
		return hostName;
	}

	public String getFileName() {
		return fileName;
	}

	public long getPosition() {
		return position;
	}

	public int getFlags() {
		return flags;
	}
}
