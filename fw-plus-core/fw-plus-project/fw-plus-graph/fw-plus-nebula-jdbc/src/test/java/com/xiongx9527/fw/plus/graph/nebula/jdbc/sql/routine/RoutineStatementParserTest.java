package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RoutineStatementParserTest {

	@Test
	public void parseCreateProcedureWithInParameterAndBodyStatements() throws Exception {
		RoutineStatement statement = RoutineStatementParser.parse(
				"CREATE PROCEDURE p(IN id VARCHAR(64)) BEGIN SELECT * FROM person WHERE id = id; UPDATE person SET touched = 1 WHERE id = id; END");

		assertEquals(RoutineStatement.Kind.CREATE_PROCEDURE, statement.getKind());
		assertEquals("p", statement.getName());
		assertEquals("p", statement.getNormalizedName());
		assertEquals(1, statement.getParameters().size());
		assertEquals("id", statement.getParameters().get(0).getName());
		assertEquals("IN", statement.getParameters().get(0).getMode());
		assertEquals("VARCHAR(64)", statement.getParameters().get(0).getTypeName());
		assertEquals(2, statement.getBodyStatements().size());
		assertEquals("SELECT * FROM person WHERE id = id", statement.getBodyStatements().get(0));
		assertEquals("UPDATE person SET touched = 1 WHERE id = id", statement.getBodyStatements().get(1));
	}

	@Test
	public void parseDropProcedureIfExists() throws Exception {
		RoutineStatement statement = RoutineStatementParser.parse("DROP PROCEDURE IF EXISTS `P`");

		assertEquals(RoutineStatement.Kind.DROP_PROCEDURE, statement.getKind());
		assertEquals("P", statement.getName());
		assertEquals("p", statement.getNormalizedName());
		assertTrue(statement.isIfExists());
	}

	@Test
	public void parseCallWithArguments() throws Exception {
		RoutineStatement statement = RoutineStatementParser.parse("CALL p('v1', 18, NULL)");

		assertEquals(RoutineStatement.Kind.CALL_PROCEDURE, statement.getKind());
		assertEquals("p", statement.getName());
		assertEquals(3, statement.getArguments().size());
		assertEquals("v1", statement.getArguments().get(0));
		assertEquals(Integer.valueOf(18), statement.getArguments().get(1));
		assertEquals(null, statement.getArguments().get(2));
	}

	@Test
	public void parseIfBlockAsSingleBodyStatement() throws Exception {
		RoutineStatement statement = RoutineStatementParser.parse(
				"CREATE PROCEDURE p(IN mode VARCHAR(8)) BEGIN DECLARE target VARCHAR(8) DEFAULT 'B'; SET target = mode; IF target = 'A' THEN SELECT * FROM person WHERE id = target; ELSEIF target = 'B' THEN UPDATE person SET touched = 1 WHERE id = target; ELSE SELECT * FROM fallback; END IF; END");

		assertEquals(3, statement.getBodyStatements().size());
		assertEquals("DECLARE target VARCHAR(8) DEFAULT 'B'", statement.getBodyStatements().get(0));
		assertEquals("SET target = mode", statement.getBodyStatements().get(1));
		assertTrue(statement.getBodyStatements().get(2).startsWith("IF target = 'A' THEN"));
		assertTrue(statement.getBodyStatements().get(2).endsWith("END IF"));
	}

	@Test
	public void rejectUnsupportedRoutineFeaturesExplicitly() throws Exception {
		assertUnsupported("CREATE FUNCTION f() RETURNS INT RETURN 1");
		assertUnsupported("CREATE PROCEDURE p(OUT id VARCHAR(64)) BEGIN SELECT 1; END");
		assertUnsupported("CREATE PROCEDURE p(INOUT id VARCHAR(64)) BEGIN SELECT 1; END");
		assertUnsupported("CREATE PROCEDURE p() BEGIN DECLARE c CURSOR FOR SELECT 1; END");
		assertUnsupported("CREATE PROCEDURE p() BEGIN DECLARE CONTINUE HANDLER FOR SQLEXCEPTION SELECT 1; END");
		assertUnsupported("CREATE PROCEDURE p() BEGIN PREPARE stmt FROM 'SELECT 1'; END");
		assertUnsupported("CREATE PROCEDURE p() BEGIN START TRANSACTION; SELECT 1; END");
		assertUnsupported("CREATE PROCEDURE p() BEGIN WHILE 1 = 1 DO SELECT 1; END WHILE; END");
		assertUnsupported("CREATE PROCEDURE p() BEGIN LOOP SELECT 1; END LOOP; END");
	}

	@Test
	public void unsupportedKeywordsInsideStringLiteralsShouldNotBeRejected() throws Exception {
		RoutineStatement statement = RoutineStatementParser.parse(
				"CREATE PROCEDURE p() BEGIN SELECT 'COMMIT'; SELECT 'PREPARE stmt FROM ''SELECT 1'''; END");

		assertEquals(RoutineStatement.Kind.CREATE_PROCEDURE, statement.getKind());
		assertEquals(2, statement.getBodyStatements().size());
		assertEquals("SELECT 'COMMIT'", statement.getBodyStatements().get(0));
		assertEquals("SELECT 'PREPARE stmt FROM ''SELECT 1'''", statement.getBodyStatements().get(1));
	}

	private void assertUnsupported(String sql) throws Exception {
		try {
			RoutineStatementParser.parse(sql);
			fail("SQL must fail: " + sql);
		}
		catch (SQLException exception) {
			assertEquals("42000", exception.getSQLState());
			assertTrue(exception.getMessage().contains("网关 routine 子集不支持"));
		}
	}
}
