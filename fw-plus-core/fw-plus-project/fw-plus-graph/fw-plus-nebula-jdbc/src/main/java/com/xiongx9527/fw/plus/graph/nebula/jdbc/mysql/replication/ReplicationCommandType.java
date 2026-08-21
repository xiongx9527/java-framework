package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public enum ReplicationCommandType {
	COM_BINLOG_DUMP(0x12),
	COM_REGISTER_SLAVE(0x15),
	COM_BINLOG_DUMP_GTID(0x1e),
	UNKNOWN(-1);

	private final int commandCode;

	ReplicationCommandType(int commandCode) {
		this.commandCode = commandCode;
	}

	public int getCommandCode() {
		return commandCode;
	}

	public static ReplicationCommandType fromCommandCode(int commandCode) {
		for (ReplicationCommandType type : values()) {
			if (type.commandCode == commandCode) {
				return type;
			}
		}
		return UNKNOWN;
	}
}
