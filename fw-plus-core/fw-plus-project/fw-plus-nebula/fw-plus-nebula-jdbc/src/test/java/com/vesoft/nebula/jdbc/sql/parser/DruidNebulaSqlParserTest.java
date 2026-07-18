package com.vesoft.nebula.jdbc.sql.parser;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaDeleteStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaExpression;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaInsertStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaMetadataStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSchemaStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaTableSource;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaUpdateStatement;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.ErrorKind;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DruidNebulaSqlParserTest {

	private final NebulaSqlParser parser = new DruidNebulaSqlParser();

	@Test
	public void parseSelectBasicShape() throws Exception {
		NebulaSqlParseResult result = parser.parse("SELECT id, name FROM person WHERE age > ? ORDER BY name LIMIT 10");

		NebulaSelectStatement select = (NebulaSelectStatement) result.getStatement();
		assertEquals(2, select.getSelectItems().size());
		assertEquals("person", select.getFrom().getName());
		assertEquals(NebulaExpression.Type.BINARY, select.getWhere().getType());
		assertEquals(1, select.getOrderBy().size());
		assertEquals(Integer.valueOf(10), select.getLimit().getRowCount());
		assertEquals(1, result.getParameters().size());
	}

	@Test
	public void parseJoinRelation() throws Exception {
		NebulaSelectStatement select = select("SELECT p.id, o.amount FROM person p JOIN orders o ON p.id = o.person_id");

		assertEquals(NebulaTableSource.Type.JOIN, select.getFrom().getType());
		assertEquals("p.id", select.getSelectItems().get(0).getExpression().getName());
		assertEquals("person", select.getFrom().getLeft().getName());
		assertEquals("p", select.getFrom().getLeft().getAlias());
		assertEquals("orders", select.getFrom().getRight().getName());
		assertEquals(NebulaExpression.Type.BINARY, select.getFrom().getCondition().getType());
	}

	@Test
	public void parseSubquerySource() throws Exception {
		NebulaSelectStatement select = select("SELECT t.id FROM (SELECT id FROM person WHERE age > 18) t");

		assertEquals(NebulaTableSource.Type.SUBQUERY, select.getFrom().getType());
		assertEquals("t", select.getFrom().getAlias());
		assertNotNull(select.getFrom().getSubquery());
		assertEquals("person", select.getFrom().getSubquery().getFrom().getName());
	}

	@Test
	public void parseUnionQuery() throws Exception {
		NebulaSelectStatement select = select("SELECT id FROM person_a UNION SELECT id FROM person_b");

		assertNotNull(select.getUnion());
		assertEquals("person_a", select.getUnion().getLeft().getFrom().getName());
		assertEquals("person_b", select.getUnion().getRight().getFrom().getName());
	}

	@Test
	public void parseGroupByHavingAndAggregateAlias() throws Exception {
		NebulaSelectStatement select = select("SELECT city, COUNT(*) c FROM person GROUP BY city HAVING COUNT(*) > 1");

		assertEquals(1, select.getGroupBy().size());
		assertEquals("c", select.getSelectItems().get(1).getAlias());
		assertTrue(select.getSelectItems().get(1).getExpression().isAggregate());
		assertEquals(NebulaExpression.Type.BINARY, select.getHaving().getType());
	}

	@Test
	public void parseInListParametersInOrder() throws Exception {
		NebulaSqlParseResult result = parser.parse("SELECT * FROM person WHERE id IN (?, ?, ?)");
		NebulaSelectStatement select = (NebulaSelectStatement) result.getStatement();

		assertEquals(NebulaExpression.Type.IN_LIST, select.getWhere().getType());
		assertEquals(3, result.getParameters().size());
		assertEquals(1, result.getParameters().get(0).getIndex());
		assertEquals(2, result.getParameters().get(1).getIndex());
		assertEquals(3, result.getParameters().get(2).getIndex());
	}

	@Test
	public void parseLikeAndIsNullExplicitly() throws Exception {
		NebulaSelectStatement likeSelect = select("SELECT * FROM person WHERE name LIKE ?");
		assertEquals(NebulaExpression.Type.LIKE, likeSelect.getWhere().getType());

		NebulaSelectStatement isNullSelect = select("SELECT * FROM person WHERE deleted_at IS NULL");
		assertEquals(NebulaExpression.Type.IS_NULL, isNullSelect.getWhere().getType());
	}

	@Test
	public void parseInsertUpdateDelete() throws Exception {
		NebulaInsertStatement insert = (NebulaInsertStatement) parser.parse("INSERT INTO person (id, name) VALUES (?, ?)").getStatement();
		assertEquals("person", insert.getTableName());
		assertEquals(2, insert.getColumns().size());
		assertEquals(2, insert.getValues().get(0).size());

		NebulaUpdateStatement update = (NebulaUpdateStatement) parser.parse("UPDATE person SET name = ? WHERE id = ?").getStatement();
		assertEquals("person", update.getTable().getName());
		assertEquals(1, update.getItems().size());
		assertEquals(NebulaExpression.Type.BINARY, update.getWhere().getType());

		NebulaDeleteStatement delete = (NebulaDeleteStatement) parser.parse("DELETE FROM person WHERE id = ?").getStatement();
		assertEquals("person", delete.getTable().getName());
		assertEquals(NebulaExpression.Type.BINARY, delete.getWhere().getType());
	}

	@Test
	public void parseCreateDropAndMetadata() throws Exception {
		NebulaSchemaStatement create = (NebulaSchemaStatement) parser.parse(
				"CREATE TABLE person (id BIGINT PRIMARY KEY, name VARCHAR(64))").getStatement();
		assertEquals(StatementKind.CREATE_SCHEMA, create.getKind());
		assertEquals("person", create.getTableName());
		assertEquals(2, create.getColumns().size());

		NebulaSchemaStatement drop = (NebulaSchemaStatement) parser.parse("DROP TABLE person").getStatement();
		assertEquals(StatementKind.DROP_SCHEMA, drop.getKind());
		assertEquals("person", drop.getTableName());

		NebulaMetadataStatement showTables = (NebulaMetadataStatement) parser.parse("SHOW TABLES").getStatement();
		assertEquals(StatementKind.METADATA, showTables.getKind());
		assertNull(showTables.getTargetName());

		NebulaMetadataStatement showColumns = (NebulaMetadataStatement) parser.parse("SHOW COLUMNS FROM person").getStatement();
		assertEquals("person", showColumns.getTargetName());

		NebulaMetadataStatement describe = (NebulaMetadataStatement) parser.parse("DESCRIBE person").getStatement();
		assertEquals("person", describe.getTargetName());
	}

	@Test
	public void rejectInvalidUnsupportedDeferredAndMultiStatement() throws Exception {
		assertError("SELECT FROM", ErrorKind.SYNTAX_ERROR);
		assertError("CALL refresh_person()", ErrorKind.UNSUPPORTED_FEATURE);
		assertError("CREATE TRIGGER trg BEFORE INSERT ON person FOR EACH ROW SET @x = 1", ErrorKind.UNSUPPORTED_FEATURE);
		assertError("SELECT 1; SELECT 2", ErrorKind.UNSUPPORTED_FEATURE);
	}

	private NebulaSelectStatement select(String sql) throws Exception {
		return (NebulaSelectStatement) parser.parse(sql).getStatement();
	}

	private void assertError(String sql, ErrorKind expected) throws Exception {
		try {
			parser.parse(sql);
			fail("SQL must fail: " + sql);
		}
		catch (NebulaSqlParseException ex) {
			assertEquals(expected, ex.getError().getKind());
		}
	}
}
