package com.vesoft.nebula.jdbc.mysql.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;

public class NebulaMysqlGatewayConnectionHandler implements Runnable {
	private final Socket socket;

	public NebulaMysqlGatewayConnectionHandler(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void run() {
		closeQuietly(socket);
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
