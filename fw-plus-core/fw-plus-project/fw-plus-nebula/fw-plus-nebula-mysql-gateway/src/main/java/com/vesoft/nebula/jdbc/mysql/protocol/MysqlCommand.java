package com.vesoft.nebula.jdbc.mysql.protocol;

public final class MysqlCommand {
	public static final int COM_QUIT = 0x01;
	public static final int COM_INIT_DB = 0x02;
	public static final int COM_QUERY = 0x03;
	public static final int COM_FIELD_LIST = 0x04;
	public static final int COM_PING = 0x0e;
	public static final int COM_STMT_PREPARE = 0x16;
	public static final int COM_STMT_EXECUTE = 0x17;

	private MysqlCommand() {
	}
}
