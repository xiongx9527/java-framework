package com.vesoft.nebula.jdbc.mysql.protocol;

public final class MysqlCapability {
	public static final int CLIENT_PROTOCOL_41 = 0x00000200;
	public static final int CLIENT_SECURE_CONNECTION = 0x00008000;
	public static final int CLIENT_PLUGIN_AUTH = 0x00080000;
	public static final int CLIENT_CONNECT_WITH_DB = 0x00000008;

	private MysqlCapability() {
	}
}
