package com.xiongx9527.fw.plus.graph.nebula.jdbc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication.BinlogStreamException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication.ReplicationError;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.mysql.replication.ReplicationException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.ErrorKind;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.NebulaSqlParseError;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaQueryConversionException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaQueryErrorCode;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine.RoutineException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaWriteConversionException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaWriteErrorCode;

/**
 * 锁定 nebula-jdbc 各异常的固定状态码（SERVER/DATABASE/001-006）。
 */
public class NebulaJdbcExceptionCodeTest {

	@Test
	public void routineExceptionCarriesDatabaseCode() {
		assertEquals(50_009_001, RoutineException.CODE);
		assertEquals(50_009_001, RoutineException.syntax("parse error").getCode());
		assertEquals(50_009_001, RoutineException.unsupported("window").getCode());
	}

	@Test
	public void nebulaSqlParseExceptionCarriesDatabaseCode() {
		assertEquals(50_009_002, NebulaSqlParseException.CODE);
		NebulaSqlParseError error = new NebulaSqlParseError(ErrorKind.SYNTAX_ERROR, "parse failed",
				null, null, null, null, null);
		assertEquals(50_009_002, new NebulaSqlParseException(error).getCode());
	}

	@Test
	public void nebulaQueryConversionExceptionCarriesDatabaseCode() {
		assertEquals(50_009_003, NebulaQueryConversionException.CODE);
		assertEquals(50_009_003,
				NebulaQueryConversionException.of(NebulaQueryErrorCode.INVALID_INPUT, "bad input").getCode());
	}

	@Test
	public void nebulaWriteConversionExceptionCarriesDatabaseCode() {
		assertEquals(50_009_004, NebulaWriteConversionException.CODE);
		assertEquals(50_009_004,
				NebulaWriteConversionException
						.of(NebulaWriteErrorCode.NEBULA_SQL_WRITE_MISSING_ID, "missing id").getCode());
	}

	@Test
	public void binlogStreamExceptionCarriesDatabaseCode() {
		assertEquals(50_009_005, BinlogStreamException.CODE);
		assertEquals(50_009_005, BinlogStreamException.positionUnavailable("no position").getCode());
	}

	@Test
	public void replicationExceptionCarriesDatabaseCode() {
		assertEquals(50_009_006, ReplicationException.CODE);
		ReplicationError error = new ReplicationError(1304, "42000", "routine not found");
		assertEquals(50_009_006, new ReplicationException(error).getCode());
	}
}
