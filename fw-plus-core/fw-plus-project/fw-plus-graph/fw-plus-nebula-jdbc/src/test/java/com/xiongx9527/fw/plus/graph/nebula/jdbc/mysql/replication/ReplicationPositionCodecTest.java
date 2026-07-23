package com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReplicationPositionCodecTest {

	@Test
	public void decodeShouldCreateStableStreamPositionFromFileAndOffset() throws Exception {
		ReplicationPositionCodec codec = new ReplicationPositionCodec();

		ReplicationStreamPosition position = codec.decode("nebula-binlog.000001", 42L);

		assertEquals("nebula-binlog.000001", position.getFileName());
		assertEquals(42L, position.getOffset());
		assertEquals("nebula-binlog.000001:42", position.getToken());
	}

	@Test
	public void decodeShouldTreatHeaderOffsetAsBeginning() throws Exception {
		ReplicationPositionCodec codec = new ReplicationPositionCodec();

		ReplicationStreamPosition position = codec.decode("nebula-binlog.000001", 4L);

		assertEquals("nebula-binlog.000001:4", position.getToken());
	}

	@Test
	public void decodeShouldRejectUnknownFileView() {
		assertPositionError("mysql-bin.000001", 4L, "Could not parse requested binlog position");
	}

	@Test
	public void decodeShouldRejectOffsetsBeforeMysqlBinlogHeader() {
		assertPositionError("nebula-binlog.000001", 3L, "Could not parse requested binlog position");
	}

	private void assertPositionError(String fileName, long offset, String message) {
		try {
			new ReplicationPositionCodec().decode(fileName, offset);
			fail("Expected ReplicationException");
		} catch (ReplicationException exception) {
			assertEquals(1236, exception.getError().getErrorCode());
			assertEquals("HY000", exception.getError().getSqlState());
			assertEquals(message, exception.getError().getMessage());
		}
	}
}
