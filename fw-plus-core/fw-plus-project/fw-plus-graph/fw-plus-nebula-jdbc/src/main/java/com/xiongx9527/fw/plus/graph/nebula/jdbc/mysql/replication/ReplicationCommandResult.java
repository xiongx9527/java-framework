package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import java.util.Collections;
import java.util.List;

public class ReplicationCommandResult {
	private final boolean ok;
	private final ReplicationError error;
	private final List<ReplicationEventPacket> eventPackets;

	private ReplicationCommandResult(boolean ok, ReplicationError error, List<ReplicationEventPacket> eventPackets) {
		this.ok = ok;
		this.error = error;
		this.eventPackets = eventPackets;
	}

	public static ReplicationCommandResult ok() {
		return new ReplicationCommandResult(true, null, Collections.<ReplicationEventPacket>emptyList());
	}

	public static ReplicationCommandResult events(List<ReplicationEventPacket> eventPackets) {
		return new ReplicationCommandResult(true, null, Collections.unmodifiableList(eventPackets));
	}

	public static ReplicationCommandResult error(ReplicationError error) {
		return new ReplicationCommandResult(false, error, Collections.<ReplicationEventPacket>emptyList());
	}

	public boolean isOk() {
		return ok;
	}

	public ReplicationError getError() {
		return error;
	}

	public List<ReplicationEventPacket> getEventPackets() {
		return eventPackets;
	}
}
