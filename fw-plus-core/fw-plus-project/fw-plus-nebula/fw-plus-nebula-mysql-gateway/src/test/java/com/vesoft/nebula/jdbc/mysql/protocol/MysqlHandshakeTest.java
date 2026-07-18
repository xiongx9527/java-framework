package com.vesoft.nebula.jdbc.mysql.protocol;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MysqlHandshakeTest {
	@Test
	public void shouldEncodeProtocolV10Handshake() {
		byte[] seed = "12345678901234567890".getBytes(MysqlCharsets.UTF8);
		MysqlHandshakePacket handshake = new MysqlHandshakePacket(
				"5.7.0-fw-nebula-gateway",
				11L,
				seed,
				MysqlCapability.CLIENT_PROTOCOL_41 | MysqlCapability.CLIENT_SECURE_CONNECTION | MysqlCapability.CLIENT_PLUGIN_AUTH,
				"mysql_native_password"
		);

		MysqlPacketReader reader = new MysqlPacketReader(handshake.toPayload());

		assertEquals(10, reader.readUnsignedByte());
		assertEquals("5.7.0-fw-nebula-gateway", reader.readNullTerminatedString());
		assertEquals(11L, reader.readUnsignedIntLE());
		assertArrayEquals("12345678".getBytes(MysqlCharsets.UTF8), reader.readBytes(8));
	}

	@Test
	public void shouldParseProtocol41HandshakeResponse() {
		byte[] auth = new byte[] {1, 2, 3};
		MysqlPacketWriter writer = new MysqlPacketWriter()
				.writeIntLE(MysqlCapability.CLIENT_PROTOCOL_41
						| MysqlCapability.CLIENT_SECURE_CONNECTION
						| MysqlCapability.CLIENT_CONNECT_WITH_DB, 4)
				.writeIntLE(1024, 4)
				.writeByte(33);
		writer.writeBytes(new byte[23]);
		writer.writeNullTerminatedString("reader");
		writer.writeByte(auth.length).writeBytes(auth);
		writer.writeNullTerminatedString("basketballplayer");

		MysqlHandshakeResponse response = MysqlHandshakeResponse.parse(writer.toByteArray());

		assertEquals("reader", response.getUsername());
		assertEquals("basketballplayer", response.getDatabase());
		assertArrayEquals(auth, response.getAuthResponse());
		assertTrue(response.hasCapability(MysqlCapability.CLIENT_CONNECT_WITH_DB));
	}
}
