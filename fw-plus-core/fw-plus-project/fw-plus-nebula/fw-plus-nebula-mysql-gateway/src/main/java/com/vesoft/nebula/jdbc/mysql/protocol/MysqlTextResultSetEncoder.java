package com.vesoft.nebula.jdbc.mysql.protocol;

import com.vesoft.nebula.jdbc.mysql.query.MysqlColumn;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;

import java.math.BigDecimal;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MysqlTextResultSetEncoder {
	private static final int MYSQL_TYPE_TINY = 1;
	private static final int MYSQL_TYPE_LONG = 3;
	private static final int MYSQL_TYPE_DOUBLE = 5;
	private static final int MYSQL_TYPE_TIMESTAMP = 7;
	private static final int MYSQL_TYPE_LONGLONG = 8;
	private static final int MYSQL_TYPE_DATETIME = 12;
	private static final int MYSQL_TYPE_VAR_STRING = 253;
	private static final int MYSQL_TYPE_NEWDECIMAL = 246;

	public List<byte[]> encodePayloads(MysqlQueryResult result) {
		List<byte[]> payloads = new ArrayList<byte[]>();
		payloads.add(new MysqlPacketWriter().writeLengthEncodedInteger(result.getColumns().size()).toByteArray());
		for (MysqlColumn column : result.getColumns()) {
			payloads.add(encodeColumn(column));
		}
		payloads.add(MysqlEofPacket.INSTANCE.toPayload());
		for (List<Object> row : result.getRows()) {
			payloads.add(encodeRow(row));
		}
		payloads.add(MysqlEofPacket.INSTANCE.toPayload());
		return payloads;
	}

	private byte[] encodeColumn(MysqlColumn column) {
		int flags = column.isReadOnly() ? 0x0001 : 0;
		return new MysqlPacketWriter()
				.writeLengthEncodedString("def")
				.writeLengthEncodedString("")
				.writeLengthEncodedString("")
				.writeLengthEncodedString("")
				.writeLengthEncodedString(column.getLabel())
				.writeLengthEncodedString(column.getName())
				.writeByte(0x0c)
				.writeIntLE(33, 2)
				.writeIntLE(1024, 4)
				.writeByte(toMysqlType(column.getJdbcType()))
				.writeIntLE(flags, 2)
				.writeByte(0)
				.writeIntLE(0, 2)
				.toByteArray();
	}

	private byte[] encodeRow(List<Object> row) {
		MysqlPacketWriter writer = new MysqlPacketWriter();
		for (Object value : row) {
			if (value == null) {
				writer.writeByte(0xfb);
			} else {
				writer.writeLengthEncodedString(toText(value));
			}
		}
		return writer.toByteArray();
	}

	private String toText(Object value) {
		if (value instanceof Boolean) {
			return ((Boolean) value) ? "1" : "0";
		}
		if (value instanceof java.sql.Timestamp) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format((Date) value);
		}
		if (value instanceof java.sql.Date || value instanceof java.sql.Time || value instanceof Date) {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value);
		}
		return String.valueOf(value);
	}

	private int toMysqlType(int jdbcType) {
		switch (jdbcType) {
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.BOOLEAN:
			case Types.BIT:
				return MYSQL_TYPE_TINY;
			case Types.INTEGER:
				return MYSQL_TYPE_LONG;
			case Types.BIGINT:
				return MYSQL_TYPE_LONGLONG;
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
				return MYSQL_TYPE_DOUBLE;
			case Types.NUMERIC:
			case Types.DECIMAL:
				return MYSQL_TYPE_NEWDECIMAL;
			case Types.TIMESTAMP:
				return MYSQL_TYPE_TIMESTAMP;
			case Types.DATE:
			case Types.TIME:
				return MYSQL_TYPE_DATETIME;
			default:
				return MYSQL_TYPE_VAR_STRING;
		}
	}
}
