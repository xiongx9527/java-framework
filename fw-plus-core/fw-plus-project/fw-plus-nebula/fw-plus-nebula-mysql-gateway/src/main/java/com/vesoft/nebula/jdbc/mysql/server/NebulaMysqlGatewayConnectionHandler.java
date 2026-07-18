package com.vesoft.nebula.jdbc.mysql.server;

import com.vesoft.nebula.jdbc.mysql.command.MysqlCommandResult;
import com.vesoft.nebula.jdbc.mysql.command.MysqlCommandDispatcher;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCapability;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCharsets;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlHandshakePacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlHandshakeResponse;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlOkPacket;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlTextResultSetEncoder;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class NebulaMysqlGatewayConnectionHandler implements Runnable {
	private static final AtomicLong CONNECTION_IDS = new AtomicLong(1);
	private static final int SERVER_CAPABILITIES = MysqlCapability.CLIENT_LONG_PASSWORD
			| MysqlCapability.CLIENT_LONG_FLAG
			| MysqlCapability.CLIENT_CONNECT_WITH_DB
			| MysqlCapability.CLIENT_PROTOCOL_41
			| MysqlCapability.CLIENT_TRANSACTIONS
			| MysqlCapability.CLIENT_SECURE_CONNECTION
			| MysqlCapability.CLIENT_PLUGIN_AUTH;
	private static final byte[] AUTH_SEED = "nebula-mysql-gateway".getBytes(MysqlCharsets.UTF8);

	private final Socket socket;
	private final MysqlCommandDispatcher commandDispatcher;

	public NebulaMysqlGatewayConnectionHandler(Socket socket) {
		this(socket, null);
	}

	public NebulaMysqlGatewayConnectionHandler(Socket socket, MysqlCommandDispatcher commandDispatcher) {
		this.socket = socket;
		this.commandDispatcher = commandDispatcher;
	}

	@Override
	public void run() {
		if (commandDispatcher == null) {
			closeQuietly(socket);
			return;
		}
		try {
			handleConnection();
		} catch (IOException ignored) {
			// A client-side disconnect is scoped to this connection thread.
		} finally {
			closeQuietly(socket);
		}
	}

	private void handleConnection() throws IOException {
		InputStream input = socket.getInputStream();
		OutputStream output = socket.getOutputStream();
		MysqlProtocolSession session = new MysqlProtocolSession(CONNECTION_IDS.getAndIncrement());

		writePacket(output, new MysqlHandshakePacket(
				"5.7.0-nebula-gateway",
				session.getConnectionId(),
				AUTH_SEED,
				SERVER_CAPABILITIES,
				"mysql_native_password").toPayload(), 0);
		byte[] response = readPacket(input);
		if (response == null) {
			return;
		}
		authenticate(session, response);
		writePacket(output, MysqlOkPacket.success().toPayload(), 2);

		while (!session.isClosed()) {
			byte[] commandPayload = readPacket(input);
			if (commandPayload == null || commandPayload.length == 0) {
				return;
			}
			int command = commandPayload[0] & 0xff;
			String argument = parseArgument(command, commandPayload);
			MysqlCommandResult result = commandDispatcher.dispatch(session, command, argument);
			writeResult(output, result);
		}
	}

	private void authenticate(MysqlProtocolSession session, byte[] payload) {
		MysqlHandshakeResponse response = MysqlHandshakeResponse.parse(payload);
		session.authenticate(response.getUsername(), response.getDatabase(), response.getCapabilityFlags());
	}

	private String parseArgument(int command, byte[] payload) {
		if (command == MysqlCommand.COM_STMT_RESET || command == MysqlCommand.COM_STMT_CLOSE) {
			if (payload.length < 5) {
				return "";
			}
			long statementId = ((long) payload[1] & 0xff)
					| (((long) payload[2] & 0xff) << 8)
					| (((long) payload[3] & 0xff) << 16)
					| (((long) payload[4] & 0xff) << 24);
			return String.valueOf(statementId);
		}
		return new String(payload, 1, payload.length - 1, MysqlCharsets.UTF8);
	}

	private void writeResult(OutputStream output, MysqlCommandResult result) throws IOException {
		if (result.getType() == MysqlCommandResult.Type.RESULT_SET) {
			List<byte[]> payloads = new MysqlTextResultSetEncoder().encodePayloads(result.getResultSet());
			for (int i = 0; i < payloads.size(); i++) {
				writePacket(output, payloads.get(i), i + 1);
			}
			return;
		}
		writePacket(output, result.getPayload(), 1);
	}

	private byte[] readPacket(InputStream input) throws IOException {
		byte[] header = readFully(input, 4);
		if (header == null) {
			return null;
		}
		int length = (header[0] & 0xff) | ((header[1] & 0xff) << 8) | ((header[2] & 0xff) << 16);
		return readFully(input, length);
	}

	private byte[] readFully(InputStream input, int length) throws IOException {
		byte[] buffer = new byte[length];
		int offset = 0;
		while (offset < length) {
			int read = input.read(buffer, offset, length - offset);
			if (read < 0) {
				return null;
			}
			offset += read;
		}
		return buffer;
	}

	private void writePacket(OutputStream output, byte[] payload, int sequenceId) throws IOException {
		int length = payload == null ? 0 : payload.length;
		output.write(length & 0xff);
		output.write((length >>> 8) & 0xff);
		output.write((length >>> 16) & 0xff);
		output.write(sequenceId & 0xff);
		if (payload != null) {
			output.write(payload);
		}
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
