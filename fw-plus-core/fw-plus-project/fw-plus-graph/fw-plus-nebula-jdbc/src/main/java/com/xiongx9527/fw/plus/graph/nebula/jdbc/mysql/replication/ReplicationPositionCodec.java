package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

public class ReplicationPositionCodec {
	private static final String FILE_PREFIX = "nebula-binlog.";
	private static final long MIN_OFFSET = 4L;

	public ReplicationStreamPosition decode(String fileName, long offset) throws ReplicationException {
		if (fileName == null || !fileName.startsWith(FILE_PREFIX) || offset < MIN_OFFSET) {
			throw new ReplicationException(ReplicationErrorMapper.invalidPosition());
		}
		return new ReplicationStreamPosition(fileName, offset);
	}
}
