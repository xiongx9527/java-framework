package com.vesoft.nebula.jdbc.mysql.replication;

import java.util.ArrayList;
import java.util.List;

public class ReplicationCommandHandler {
	private static final int DEFAULT_BATCH_SIZE = 100;
	private static final long DEFAULT_TIMEOUT_MILLIS = 0L;

	private final BinlogStreamReader streamReader;
	private final ReplicationPositionCodec positionCodec;
	private final boolean enabled;

	public ReplicationCommandHandler(BinlogStreamReader streamReader, ReplicationPositionCodec positionCodec,
			boolean enabled) {
		this.streamReader = streamReader;
		this.positionCodec = positionCodec;
		this.enabled = enabled;
	}

	public ReplicationCommandResult handle(ReplicationCommandRequest request, ReplicationSessionState session) {
		if (!session.isAuthenticated()) {
			return ReplicationCommandResult.error(ReplicationErrorMapper.accessDenied());
		}
		if (!enabled) {
			return ReplicationCommandResult.error(ReplicationErrorMapper.replicationDisabled());
		}
		if (request.getType() == ReplicationCommandType.COM_REGISTER_SLAVE) {
			session.registerReplica(request.getServerId(), request.getHostName());
			return ReplicationCommandResult.ok();
		}
		if (request.getType() == ReplicationCommandType.COM_BINLOG_DUMP) {
			return dump(request, session);
		}
		if (request.getType() == ReplicationCommandType.COM_BINLOG_DUMP_GTID) {
			return ReplicationCommandResult.error(ReplicationErrorMapper.gtidUnsupported());
		}
		return ReplicationCommandResult.error(ReplicationErrorMapper.replicationCommandUnsupported());
	}

	private ReplicationCommandResult dump(ReplicationCommandRequest request, ReplicationSessionState session) {
		try {
			ReplicationStreamPosition startPosition = positionCodec.decode(request.getFileName(), request.getPosition());
			BinlogReadRequest readRequest = new BinlogReadRequest(
					session.getConnectionId(),
					session.getUserName(),
					request.getServerId(),
					startPosition,
					request.getFlags(),
					true,
					DEFAULT_BATCH_SIZE,
					DEFAULT_TIMEOUT_MILLIS
			);
			BinlogStreamCursor cursor = streamReader.open(readRequest);
			session.startStreaming(cursor, request.getServerId(), request.getFileName(), request.getPosition(),
					request.getFlags());
			List<ReplicationEventPacket> packets = new ArrayList<ReplicationEventPacket>();
			packets.add(ReplicationEventPacket.formatDescription(startPosition));
			List<BinlogStreamEvent> events = cursor.read(DEFAULT_BATCH_SIZE, DEFAULT_TIMEOUT_MILLIS);
			for (BinlogStreamEvent event : events) {
				packets.add(ReplicationEventPacket.from(event));
				session.markSent(event.getNextPosition());
			}
			return ReplicationCommandResult.events(packets);
		} catch (ReplicationException exception) {
			return ReplicationCommandResult.error(exception.getError());
		} catch (BinlogStreamException exception) {
			return ReplicationCommandResult.error(ReplicationErrorMapper.fromStreamException(exception));
		}
	}
}
