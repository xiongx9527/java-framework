package com.vesoft.nebula.jdbc.mysql.protocol;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class MysqlPacketCodecTest {
	@Test
	public void shouldEncodeAndDecodePacketHeader() {
		MysqlPacket packet = new MysqlPacket(2, new byte[] {1, 2, 3});

		byte[] encoded = packet.toBytes();
		MysqlPacket decoded = MysqlPacket.fromBytes(encoded);

		assertArrayEquals(new byte[] {3, 0, 0, 2, 1, 2, 3}, encoded);
		assertEquals(2, decoded.getSequenceId());
		assertArrayEquals(new byte[] {1, 2, 3}, decoded.getPayload());
	}

	@Test
	public void shouldRoundTripLengthEncodedIntegersAndStrings() {
		MysqlPacketWriter writer = new MysqlPacketWriter();
		writer.writeLengthEncodedInteger(250);
		writer.writeLengthEncodedInteger(251);
		writer.writeLengthEncodedInteger(65536);
		writer.writeNullTerminatedString("reader");

		MysqlPacketReader reader = new MysqlPacketReader(writer.toByteArray());

		assertEquals(250L, reader.readLengthEncodedInteger());
		assertEquals(251L, reader.readLengthEncodedInteger());
		assertEquals(65536L, reader.readLengthEncodedInteger());
		assertEquals("reader", reader.readNullTerminatedString());
	}

	@Test
	public void shouldEncodeOkErrAndEofPayloads() {
		assertArrayEquals(new byte[] {0, 0, 0, 2, 0, 0, 0}, MysqlOkPacket.success().toPayload());

		byte[] err = MysqlErrPacket.of(1235, "HY000", "Command is not supported").toPayload();
		assertEquals(0xff, err[0] & 0xff);
		assertEquals(1235, (err[1] & 0xff) | ((err[2] & 0xff) << 8));
		assertEquals('#', (char) err[3]);
		assertEquals("HY000", new String(err, 4, 5, MysqlCharsets.UTF8));

		assertArrayEquals(new byte[] {(byte) 0xfe, 0, 0, 2, 0}, MysqlEofPacket.INSTANCE.toPayload());
	}
}
