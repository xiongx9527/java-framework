package com.vesoft.nebula.jdbc.mysql.replication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ReplicationCommandHandlerTest {

	@Test
	public void registerSlaveShouldStoreReplicaContext() {
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new EmptyBinlogStreamReader(),
				new ReplicationPositionCodec(),
				true
		);
		ReplicationSessionState session = authenticatedSession();

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.registerSlave(17L, "replica-host"),
				session
		);

		assertTrue(result.isOk());
		assertEquals(17L, session.getReplicaServerId());
		assertEquals("replica-host", session.getReplicaHostName());
		assertFalse(session.isStreaming());
	}

	@Test
	public void disabledReplicationShouldReturnExplicitUnsupportedError() {
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new EmptyBinlogStreamReader(),
				new ReplicationPositionCodec(),
				false
		);

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.registerSlave(17L, "replica-host"),
				authenticatedSession()
		);

		assertError(result, 1235, "HY000", "Replication subset is not enabled");
	}

	@Test
	public void unauthenticatedReplicationCommandShouldReturnAccessDenied() {
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new EmptyBinlogStreamReader(),
				new ReplicationPositionCodec(),
				true
		);

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.binlogDump("nebula-binlog.000001", 4L, 0, 17L),
				new ReplicationSessionState("connection-1")
		);

		assertError(result, 1045, "28000", "Access denied");
	}

	@Test
	public void gtidDumpShouldReturnExplicitUnsupportedError() {
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new EmptyBinlogStreamReader(),
				new ReplicationPositionCodec(),
				true
		);

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.unsupported(ReplicationCommandType.COM_BINLOG_DUMP_GTID),
				authenticatedSession()
		);

		assertError(result, 1235, "HY000", "GTID replication is not supported");
	}

	@Test
	public void unknownReplicationCommandShouldReturnExplicitUnsupportedError() {
		ReplicationCommandHandler handler = new ReplicationCommandHandler(
				new EmptyBinlogStreamReader(),
				new ReplicationPositionCodec(),
				true
		);

		ReplicationCommandResult result = handler.handle(
				ReplicationCommandRequest.unknown(0x55),
				authenticatedSession()
		);

		assertError(result, 1235, "HY000", "Replication command is not supported");
	}

	@Test
	public void parserShouldDecodeBinlogDumpPacket() throws Exception {
		byte[] packet = new byte[] {
				0x12,
				0x2a, 0x00, 0x00, 0x00,
				0x01, 0x00,
				0x11, 0x00, 0x00, 0x00,
				'n', 'e', 'b', 'u', 'l', 'a', '-', 'b', 'i', 'n', 'l', 'o', 'g', '.', '0', '0', '0', '0', '0', '1'
		};

		ReplicationCommandRequest request = new ReplicationCommandPacketParser().parse(packet);

		assertEquals(ReplicationCommandType.COM_BINLOG_DUMP, request.getType());
		assertEquals(42L, request.getPosition());
		assertEquals(1, request.getFlags());
		assertEquals(17L, request.getServerId());
		assertEquals("nebula-binlog.000001", request.getFileName());
	}

	@Test
	public void parserShouldDecodeRegisterSlavePacket() throws Exception {
		byte[] packet = new byte[] {
				0x15,
				0x11, 0x00, 0x00, 0x00,
				0x0c, 'r', 'e', 'p', 'l', 'i', 'c', 'a', '-', 'h', 'o', 's', 't',
				0x03, 'c', 'd', 'c',
				0x06, 's', 'e', 'c', 'r', 'e', 't',
				0x06, 0x0c,
				0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00
		};

		ReplicationCommandRequest request = new ReplicationCommandPacketParser().parse(packet);

		assertEquals(ReplicationCommandType.COM_REGISTER_SLAVE, request.getType());
		assertEquals(17L, request.getServerId());
		assertEquals("replica-host", request.getHostName());
	}

	private ReplicationSessionState authenticatedSession() {
		ReplicationSessionState session = new ReplicationSessionState("connection-1");
		session.markAuthenticated("cdc_user");
		return session;
	}

	private void assertError(ReplicationCommandResult result, int errorCode, String sqlState, String message) {
		assertFalse(result.isOk());
		assertEquals(errorCode, result.getError().getErrorCode());
		assertEquals(sqlState, result.getError().getSqlState());
		assertEquals(message, result.getError().getMessage());
	}
}
