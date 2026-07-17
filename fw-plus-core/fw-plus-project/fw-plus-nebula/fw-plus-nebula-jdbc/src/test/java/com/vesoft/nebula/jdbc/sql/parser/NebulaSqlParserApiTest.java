package com.vesoft.nebula.jdbc.sql.parser;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.ErrorKind;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaSqlParserApiTest {

	@Test
	public void parseSelectExposesStableApi() throws Exception {
		NebulaSqlParser parser = new DruidNebulaSqlParser();

		NebulaSqlParseResult result = parser.parse("SELECT * FROM person WHERE id = ?");

		assertEquals(StatementKind.SELECT, result.getKind());
		assertEquals(1, result.getParameters().size());
		assertEquals(1, result.getParameters().get(0).getIndex());
		NebulaSqlStatement statement = result.getStatement();
		assertTrue(statement instanceof NebulaSelectStatement);
		assertFalse(result.getDiagnostic().isRawFallback());
	}

	@Test
	public void rejectBlankSqlWithStructuredError() {
		NebulaSqlParser parser = new DruidNebulaSqlParser();

		try {
			parser.parse("   ");
			fail("blank SQL must be rejected");
		}
		catch (NebulaSqlParseException ex) {
			assertEquals(ErrorKind.INVALID_INPUT, ex.getError().getKind());
			assertNotNull(ex.getError().getMessage());
		}
	}
}
