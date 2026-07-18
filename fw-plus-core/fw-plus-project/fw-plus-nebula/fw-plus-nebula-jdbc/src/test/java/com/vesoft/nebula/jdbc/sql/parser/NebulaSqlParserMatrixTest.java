package com.vesoft.nebula.jdbc.sql.parser;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaExpression;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaInsertStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaMetadataStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSchemaStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectItem;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSelectStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaTableSource;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.ErrorKind;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaSqlParserMatrixTest {

	private final NebulaSqlParser parser = new DruidNebulaSqlParser();

	@Test
	public void parseCoverageMatrixEntries() throws Exception {
		List<MatrixEntry> entries = loadEntries();

		for (MatrixEntry entry : entries) {
			if ("required".equals(entry.supportLevel)) {
				NebulaSqlParseResult result = parser.parse(entry.exampleSql);
				assertNotNull(entry.id, result.getStatement());
				assertFalse(entry.id, result.getDiagnostic().isRawFallback());
				assertTrue(entry.id + " expected " + entry.nebulaAstKind, hasNebulaKind(result.getStatement(), entry.nebulaAstKind));
			}
			else if ("explicitlyUnsupported".equals(entry.supportLevel)) {
				assertError(entry, ErrorKind.UNSUPPORTED_FEATURE);
			}
			else if ("deferred".equals(entry.supportLevel)) {
				assertError(entry, ErrorKind.DEFERRED_FEATURE);
			}
			else {
				fail("Unknown support level for " + entry.id + ": " + entry.supportLevel);
			}
		}
	}

	private void assertError(MatrixEntry entry, ErrorKind expected) throws Exception {
		try {
			parser.parse(entry.exampleSql);
			fail(entry.id + " must fail with " + expected);
		}
		catch (NebulaSqlParseException ex) {
			assertEquals(entry.id, expected, ex.getError().getKind());
			assertEquals(entry.id, entry.id, ex.getError().getMatrixId());
		}
	}

	private boolean hasNebulaKind(NebulaSqlStatement statement, String expected) {
		if ("SelectStatement".equals(expected)) {
			return statement instanceof NebulaSelectStatement;
		}
		if ("InsertStatement".equals(expected)) {
			return statement instanceof NebulaInsertStatement;
		}
		if ("UpdateStatement".equals(expected)) {
			return statement.getKind() == StatementKind.UPDATE;
		}
		if ("DeleteStatement".equals(expected)) {
			return statement.getKind() == StatementKind.DELETE;
		}
		if ("CreateSchemaStatement".equals(expected)) {
			return statement instanceof NebulaSchemaStatement && statement.getKind() == StatementKind.CREATE_SCHEMA;
		}
		if ("DropSchemaStatement".equals(expected)) {
			return statement instanceof NebulaSchemaStatement && statement.getKind() == StatementKind.DROP_SCHEMA;
		}
		if ("MetadataStatement".equals(expected)) {
			return statement instanceof NebulaMetadataStatement;
		}
		if (!(statement instanceof NebulaSelectStatement)) {
			return false;
		}
		NebulaSelectStatement select = (NebulaSelectStatement) statement;
		if ("JoinRelation".equals(expected)) {
			return select.getFrom() != null && select.getFrom().getType() == NebulaTableSource.Type.JOIN;
		}
		if ("SubquerySource".equals(expected)) {
			return select.getFrom() != null && select.getFrom().getType() == NebulaTableSource.Type.SUBQUERY;
		}
		if ("UnionQuery".equals(expected)) {
			return select.getUnion() != null;
		}
		if ("GroupByClause".equals(expected)) {
			return !select.getGroupBy().isEmpty() && select.getHaving() != null;
		}
		if ("SelectItem".equals(expected)) {
			return !select.getSelectItems().isEmpty() && select.getSelectItems().get(0).getAlias() != null;
		}
		if ("ParameterExpression".equals(expected)) {
			return containsExpression(select, NebulaExpression.Type.PARAMETER);
		}
		if ("InListExpression".equals(expected)) {
			return containsExpression(select, NebulaExpression.Type.IN_LIST);
		}
		if ("BinaryExpression".equals(expected)) {
			return containsExpression(select, NebulaExpression.Type.BINARY);
		}
		if ("FunctionCallExpression".equals(expected)) {
			return containsExpression(select, NebulaExpression.Type.FUNCTION);
		}
		return false;
	}

	private boolean containsExpression(NebulaSelectStatement select, NebulaExpression.Type type) {
		for (NebulaSelectItem item : select.getSelectItems()) {
			if (containsExpression(item.getExpression(), type)) {
				return true;
			}
		}
		return containsExpression(select.getWhere(), type) || containsExpression(select.getHaving(), type);
	}

	private boolean containsExpression(NebulaExpression expression, NebulaExpression.Type type) {
		if (expression == null) {
			return false;
		}
		if (expression.getType() == type) {
			return true;
		}
		if (containsExpression(expression.getLeft(), type) || containsExpression(expression.getRight(), type)) {
			return true;
		}
		for (NebulaExpression argument : expression.getArguments()) {
			if (containsExpression(argument, type)) {
				return true;
			}
		}
		return false;
	}

	private List<MatrixEntry> loadEntries() throws Exception {
		File root = findRepositoryRoot();
		File matrix = new File(root,
				"openspec/changes/archive/2026-07-17-define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json");
		String json = new String(Files.readAllBytes(matrix.toPath()), StandardCharsets.UTF_8);
		Matcher matcher = Pattern.compile("\\{\\s*\"id\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\"exampleSql\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\"nebulaAstKind\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\"supportLevel\"\\s*:\\s*\"([^\"]+)\"[\\s\\S]*?\\}")
				.matcher(json);
		List<MatrixEntry> entries = new ArrayList<MatrixEntry>();
		while (matcher.find()) {
			entries.add(new MatrixEntry(matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)));
		}
		assertEquals(23, entries.size());
		return entries;
	}

	private File findRepositoryRoot() {
		File current = new File(System.getProperty("user.dir"));
		while (current != null) {
			if (new File(current, "openspec").isDirectory()) {
				return current;
			}
			current = current.getParentFile();
		}
		throw new IllegalStateException("Cannot locate repository root from " + System.getProperty("user.dir"));
	}

	private static class MatrixEntry {
		private final String id;
		private final String exampleSql;
		private final String nebulaAstKind;
		private final String supportLevel;

		private MatrixEntry(String id, String exampleSql, String nebulaAstKind, String supportLevel) {
			this.id = id;
			this.exampleSql = exampleSql;
			this.nebulaAstKind = nebulaAstKind;
			this.supportLevel = supportLevel;
		}
	}
}
