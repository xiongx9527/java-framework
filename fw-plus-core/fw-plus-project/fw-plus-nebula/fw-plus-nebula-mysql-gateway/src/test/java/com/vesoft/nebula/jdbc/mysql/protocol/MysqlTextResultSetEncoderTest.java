package com.vesoft.nebula.jdbc.mysql.protocol;

import com.vesoft.nebula.jdbc.mysql.query.MysqlColumn;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MysqlTextResultSetEncoderTest {
	@Test
	public void shouldEncodeTextResultSetPayloads() {
		MysqlQueryResult result = MysqlQueryResult.of(
				Arrays.asList(
						new MysqlColumn("name", "name", Types.VARCHAR, true, true),
						new MysqlColumn("age", "age", Types.INTEGER, true, true),
						new MysqlColumn("active", "active", Types.BOOLEAN, true, true)
				),
				Arrays.<List<Object>>asList(
						Arrays.<Object>asList("Tim", 42, true),
						Arrays.<Object>asList(null, 0, false)
				)
		);

		List<byte[]> payloads = new MysqlTextResultSetEncoder().encodePayloads(result);

		assertEquals(8, payloads.size());
		assertEquals(3L, new MysqlPacketReader(payloads.get(0)).readLengthEncodedInteger());
		assertEquals("def", readLengthEncodedString(new MysqlPacketReader(payloads.get(1))));

		MysqlPacketReader row = new MysqlPacketReader(payloads.get(6));
		assertEquals(0xfb, row.readUnsignedByte());
		assertEquals("0", readLengthEncodedString(row));
		assertEquals("0", readLengthEncodedString(row));
	}

	private String readLengthEncodedString(MysqlPacketReader reader) {
		int length = (int) reader.readLengthEncodedInteger();
		return new String(reader.readBytes(length), MysqlCharsets.UTF8);
	}
}
