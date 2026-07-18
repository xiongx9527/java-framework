package com.vesoft.nebula.jdbc.mysql.server;

import com.vesoft.nebula.jdbc.mysql.auth.MysqlAuthenticationResult;
import com.vesoft.nebula.jdbc.mysql.command.MysqlCommandResult;
import com.vesoft.nebula.jdbc.mysql.command.MysqlCommandDispatcher;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCapability;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlErrPacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlHandshakeHandler;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlHandshakePacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlOkPacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlPacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlPacketReader;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlTextResultSetEncoder;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

public class NebulaMysqlGatewayConnectionHandler implements Runnable {
	private static final int SERVER_CAPABILITIES = MysqlCapability.CLIENT_PROTOCOL_41
			| MysqlCapability.CLIENT_SECURE_CONNECTION
			| MysqlCapability.CLIENT_PLUGIN_AUTH
			| MysqlCapability.CLIENT_CONNECT_WITH_DB;

	private final Socket socket;
	private final long connectionId;
	private final NebulaMysqlGatewayServerConfig config;
	private final MysqlTextResultSetEncoder resultSetEncoder;

	public NebulaMysqlGatewayConnectionHandler(Socket socket) {
		this(socket, 1L, new NebulaMysqlGatewayServerConfig("0.0.0.0", 0));
	}

	public NebulaMysqlGatewayConnectionHandler(Socket socket, long connectionId, NebulaMysqlGatewayServerConfig config) {
		this.socket = socket;
		this.connectionId = connectionId;
		this.config = config;
		this.resultSetEncoder = new MysqlTextResultSetEncoder();
	}

	@Override
	public void run() {
		try {
			socket.setTcpNoDelay(true);
			serve(socket.getInputStream(), socket.getOutputStream());
		} catch (SocketException ignored) {
			// Client disconnected; the accept loop must keep running.
		} catch (IOException ignored) {
			// Protocol IO failures are scoped to this connection.
		} finally {
			closeQuietly(socket);
		}
	}

	private void serve(InputStream input, OutputStream output) throws IOException {
		MysqlProtocolSession session = new MysqlProtocolSession(connectionId);
		byte[] seed = config.getSeed();
		MysqlHandshakePacket handshake = new MysqlHandshakePacket(
				config.getServerVersion(),
				connectionId,
				seed,
				SERVER_CAPABILITIES,
				"mysql_native_password"
		);
		writePacket(output, 0, handshake.toPayload());

		MysqlPacket responsePacket = readPacket(input);
		if (responsePacket == null) {
			return;
		}
		MysqlHandshakeHandler handshakeHandler = new MysqlHandshakeHandler(config.getAuthenticator(), seed);
		MysqlAuthenticationResult authentication = handshakeHandler.authenticate(session, responsePacket.getPayload());
		if (!authentication.isAuthenticated()) {
			writePacket(output, 2, MysqlErrPacket.of(1045, "28000", authentication.getMessage()).toPayload());
			return;
		}
		writePacket(output, 2, MysqlOkPacket.success().toPayload());

		MysqlCommandDispatcher dispatcher = new MysqlCommandDispatcher(config.getQueryHandler());
		while (!session.isClosed()) {
			MysqlPacket commandPacket = readPacket(input);
			if (commandPacket == null) {
				return;
			}
			byte[] payload = commandPacket.getPayload();
			if (payload.length == 0) {
				writePacket(output, 1, MysqlErrPacket.of(1235, "HY000", "Command is not supported").toPayload());
				continue;
			}
			MysqlPacketReader reader = new MysqlPacketReader(payload);
			int command = reader.readUnsignedByte();
			String argument = reader.readRestString();
			MysqlCommandResult result = dispatcher.dispatch(session, command, argument);
			writeCommandResult(output, result);
			if (command == MysqlCommand.COM_QUIT) {
				return;
			}
		}
	}

	private MysqlPacket readPacket(InputStream input) throws IOException {
		byte[] header = readFully(input, 4);
		if (header == null) {
			return null;
		}
		int length = (header[0] & 0xff) | ((header[1] & 0xff) << 8) | ((header[2] & 0xff) << 16);
		byte[] payload = readFully(input, length);
		if (payload == null) {
			throw new EOFException("MySQL packet payload ended unexpectedly.");
		}
		byte[] packet = new byte[length + 4];
		System.arraycopy(header, 0, packet, 0, 4);
		System.arraycopy(payload, 0, packet, 4, length);
		return MysqlPacket.fromBytes(packet);
	}

	private byte[] readFully(InputStream input, int length) throws IOException {
		byte[] value = new byte[length];
		int offset = 0;
		while (offset < length) {
			int read = input.read(value, offset, length - offset);
			if (read < 0) {
				if (offset == 0) {
					return null;
				}
				throw new EOFException("MySQL packet ended unexpectedly.");
			}
			offset += read;
		}
		return value;
	}

	private void writeCommandResult(OutputStream output, MysqlCommandResult result) throws IOException {
		switch (result.getType()) {
			case OK:
			case ERROR:
				writePacket(output, 1, result.getPayload());
				return;
			case RESULT_SET:
				List<byte[]> payloads = resultSetEncoder.encodePayloads(result.getResultSet());
				for (int i = 0; i < payloads.size(); i++) {
					writePacket(output, i + 1, payloads.get(i));
				}
				return;
			default:
				writePacket(output, 1, MysqlErrPacket.of(1235, "HY000", "Command is not supported").toPayload());
		}
	}

	private void writePacket(OutputStream output, int sequenceId, byte[] payload) throws IOException {
		output.write(new MysqlPacket(sequenceId, payload).toBytes());
		output.flush();
	}

	private void closeQuietly(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException ignored) {
			// Connection cleanup should not keep the accept loop alive.
		}
	}
}
