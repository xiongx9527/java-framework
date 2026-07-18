package com.vesoft.nebula.jdbc.mysql.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicLong;

public class NebulaMysqlGatewayServer implements AutoCloseable {
	private final NebulaMysqlGatewayServerConfig config;
	private final AtomicLong connectionIds = new AtomicLong(1L);
	private ServerSocket serverSocket;
	private Thread acceptThread;
	private volatile boolean running;

	public NebulaMysqlGatewayServer(NebulaMysqlGatewayServerConfig config) {
		this.config = config;
	}

	public synchronized void start() throws IOException {
		if (running) {
			return;
		}
		serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress(config.getHost(), config.getPort()));
		running = true;
		acceptThread = new Thread(new Runnable() {
			@Override
			public void run() {
				acceptLoop();
			}
		}, "nebula-mysql-gateway-acceptor");
		acceptThread.setDaemon(true);
		acceptThread.start();
	}

	private void acceptLoop() {
		while (running) {
			try {
				Socket socket = serverSocket.accept();
				new Thread(new NebulaMysqlGatewayConnectionHandler(socket, connectionIds.getAndIncrement(), config),
						"nebula-mysql-gateway-connection").start();
			} catch (SocketException exception) {
				if (running) {
					running = false;
				}
			} catch (IOException exception) {
				if (running) {
					running = false;
				}
			}
		}
	}

	public synchronized void stop() throws IOException {
		running = false;
		if (serverSocket != null) {
			serverSocket.close();
			serverSocket = null;
		}
	}

	@Override
	public void close() throws IOException {
		stop();
	}

	public boolean isRunning() {
		return running;
	}

	public int getBoundPort() {
		return serverSocket == null ? -1 : serverSocket.getLocalPort();
	}
}
