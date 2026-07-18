package com.vesoft.nebula.jdbc.mysql.server;

import com.vesoft.nebula.jdbc.mysql.auth.MysqlNativePassword;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCapability;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCharsets;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlPacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlPacketReader;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlPacketWriter;
import org.junit.Test;

import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class NebulaMysqlGatewayServerTest {
	@Test
	public void shouldStartAndStopOnEphemeralPort() throws Exception {
		NebulaMysqlGatewayServer server = new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0));

		server.start();
		try {
			assertTrue(server.isRunning());
			assertTrue(server.getBoundPort() > 0);
		} finally {
			server.stop();
		}

		assertFalse(server.isRunning());
	}

	@Test
	public void shouldServeHandshakeAuthenticationAndSelectOneOverSocket() throws Exception {
		NebulaMysqlGatewayServer server = new NebulaMysqlGatewayServer(new NebulaMysqlGatewayServerConfig("127.0.0.1", 0));

		server.start();
		try {
			Socket socket = new Socket("127.0.0.1", server.getBoundPort());
			socket.setSoTimeout(3000);
			try {
				InputStream input = socket.getInputStream();
				OutputStream output = socket.getOutputStream();

				MysqlPacket handshake = readPacket(input);
				assertNotNull(handshake);
				assertEquals(0, handshake.getSequenceId());
				byte[] seed = readSeed(handshake.getPayload());

				writePacket(output, 1, handshakeResponse("root", "", seed));
				MysqlPacket authOk = readPacket(input);
				assertEquals(2, authOk.getSequenceId());
				assertEquals(0x00, authOk.getPayload()[0] & 0xff);

				writePacket(output, 0, commandPayload(MysqlCommand.COM_QUERY, "SELECT 1"));

				MysqlPacket columnCount = readPacket(input);
				assertEquals(1L, new MysqlPacketReader(columnCount.getPayload()).readLengthEncodedInteger());
				readPacket(input);
				readPacket(input);
				MysqlPacket row = readPacket(input);
				assertEquals("1", readLengthEncodedString(new MysqlPacketReader(row.getPayload())));
				readPacket(input);

				writePacket(output, 0, commandPayload(MysqlCommand.COM_QUIT, ""));
			} finally {
				socket.close();
			}
		} finally {
			server.stop();
		}
	}

	private MysqlPacket readPacket(InputStream input) throws Exception {
		byte[] header = readFully(input, 4);
		int length = (header[0] & 0xff) | ((header[1] & 0xff) << 8) | ((header[2] & 0xff) << 16);
		byte[] payload = readFully(input, length);
		byte[] packet = new byte[length + 4];
		System.arraycopy(header, 0, packet, 0, 4);
		System.arraycopy(payload, 0, packet, 4, length);
		return MysqlPacket.fromBytes(packet);
	}

	private byte[] readFully(InputStream input, int length) throws Exception {
		byte[] value = new byte[length];
		int offset = 0;
		while (offset < length) {
			int read = input.read(value, offset, length - offset);
			if (read < 0) {
				throw new EOFException("Unexpected end of MySQL packet stream.");
			}
			offset += read;
		}
		return value;
	}

	private void writePacket(OutputStream output, int sequenceId, byte[] payload) throws Exception {
		output.write(new MysqlPacket(sequenceId, payload).toBytes());
		output.flush();
	}

	private byte[] readSeed(byte[] handshakePayload) {
		MysqlPacketReader reader = new MysqlPacketReader(handshakePayload);
		reader.readUnsignedByte();
		reader.readNullTerminatedString();
		reader.readUnsignedIntLE();
		byte[] firstSeed = reader.readBytes(8);
		reader.readUnsignedByte();
		reader.readUnsignedShortLE();
		reader.readUnsignedByte();
		reader.readUnsignedShortLE();
		reader.readUnsignedShortLE();
		int seedLength = reader.readUnsignedByte();
		reader.readBytes(10);
		byte[] restSeedWithTerminator = reader.readBytes(Math.max(13, seedLength - 8));
		int restSeedLength = restSeedWithTerminator.length;
		while (restSeedLength > 0 && restSeedWithTerminator[restSeedLength - 1] == 0) {
			restSeedLength--;
		}
		byte[] seed = new byte[firstSeed.length + restSeedLength];
		System.arraycopy(firstSeed, 0, seed, 0, firstSeed.length);
		System.arraycopy(restSeedWithTerminator, 0, seed, firstSeed.length, restSeedLength);
		return seed;
	}

	private byte[] handshakeResponse(String username, String password, byte[] seed) {
		byte[] auth = MysqlNativePassword.scramble(password, seed);
		MysqlPacketWriter writer = new MysqlPacketWriter()
				.writeIntLE(MysqlCapability.CLIENT_PROTOCOL_41
						| MysqlCapability.CLIENT_SECURE_CONNECTION
						| MysqlCapability.CLIENT_PLUGIN_AUTH, 4)
				.writeIntLE(1024 * 1024, 4)
				.writeByte(33);
		writer.writeBytes(new byte[23]);
		writer.writeNullTerminatedString(username);
		writer.writeByte(auth.length).writeBytes(auth);
		writer.writeNullTerminatedString("mysql_native_password");
		return writer.toByteArray();
	}

	private byte[] commandPayload(int command, String argument) {
		return new MysqlPacketWriter()
				.writeByte(command)
				.writeString(argument)
				.toByteArray();
	}

	private String readLengthEncodedString(MysqlPacketReader reader) {
		int length = (int) reader.readLengthEncodedInteger();
		return new String(reader.readBytes(length), MysqlCharsets.UTF8);
	}
}
