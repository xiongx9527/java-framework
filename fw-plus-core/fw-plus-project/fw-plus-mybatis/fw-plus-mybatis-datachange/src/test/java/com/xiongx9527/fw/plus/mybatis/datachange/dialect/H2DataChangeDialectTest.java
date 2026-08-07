package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class H2DataChangeDialectTest {
	private final H2DataChangeDialect dialect = new H2DataChangeDialect();

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"UPDATE sys_user SET name = ? WHERE id = ?|UPDATE|sys_user",
			"DELETE FROM sys_user WHERE id = ?|PHYSICAL_DELETE|sys_user",
			"INSERT INTO sys_user(id,name) VALUES (?,?)|INSERT|sys_user",
			"MERGE INTO sys_user target USING source_user source ON (target.id = source.id) "
					+ "WHEN MATCHED THEN UPDATE SET target.name = source.name|MERGE|sys_user"
	})
	void analyzesSupportedDml(String sql, String operation, String table) {
		AnalyzedStatement statement = dialect.analyze(context(sql)).get(0);

		assertThat(statement.getTargets()).singleElement().satisfies(target -> {
			assertThat(target.getOperation().name()).isEqualTo(operation);
			assertThat(target.getTable().getTable()).isEqualToIgnoringCase(table);
		});
	}

	@Test
	void skipsCallButKeepsFollowingDmlAtOriginalIndex() {
		List<AnalyzedStatement> statements = dialect.analyze(
				context("CALL refresh_cache(); UPDATE sys_user SET name=? WHERE id=?", "name", "id"));

		assertThat(statements).hasSize(2);
		assertThat(statements.get(0).getStatementIndex()).isZero();
		assertThat(statements.get(0).isAuditable()).isFalse();
		assertThat(statements.get(1).getStatementIndex()).isEqualTo(1);
		assertThat(statements.get(1).isAuditable()).isTrue();
	}

	@Test
	void tokenizesSemicolonsInsideCallLiteralAndCommentsWithoutSplittingStatements() {
		List<AnalyzedStatement> statements = dialect.analyze(context(
				"CALL refresh_cache('a;b') /* ; */; UPDATE sys_user SET note='x;y', name=? WHERE id=?",
				"name", "id"));

		assertThat(statements).extracting(AnalyzedStatement::getStatementIndex).containsExactly(0, 1);
		assertThat(statements.get(0).isAuditable()).isFalse();
		assertThat(statements.get(1).getTargets().get(0).getParameterMappings())
				.extracting(ParameterMapping::getProperty).containsExactly("name", "id");
	}

	@Test
	void slicesMappingsByAstParametersAndIgnoresQuestionMarkLiteral() {
		List<AnalyzedStatement> statements = dialect.analyze(context(
				"UPDATE sys_user SET name=? WHERE id=? AND note='?'; DELETE FROM sys_user WHERE id=?",
				"name", "updatedId", "deletedId"));

		assertThat(statements).extracting(AnalyzedStatement::getStatementIndex).containsExactly(0, 1);
		assertThat(statements.get(0).getTargets().get(0).getParameterMappings())
				.extracting(ParameterMapping::getProperty).containsExactly("name", "updatedId");
		assertThat(statements.get(1).getTargets().get(0).getParameterMappings())
				.extracting(ParameterMapping::getProperty).containsExactly("deletedId");
	}

	@Test
	void marksSelectAndDdlAsNonAuditable() {
		assertThat(dialect.analyze(context("SELECT * FROM sys_user"))).singleElement()
				.satisfies(statement -> assertThat(statement.isAuditable()).isFalse());
		assertThat(dialect.analyze(context("ALTER TABLE sys_user ADD COLUMN note varchar(20)"))).singleElement()
				.satisfies(statement -> assertThat(statement.isAuditable()).isFalse());
	}

	@Test
	void buildsBeforeQueryFromJoinAndWhereWithoutAssignmentParameter() {
		ChangeTarget target = dialect.analyze(context(
				"UPDATE sys_user u JOIN department d ON d.id = ? SET u.name = ? WHERE u.id = ?",
				"departmentId", "name", "userId")).get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("ID"), Arrays.asList("NAME"), 1000, true);

		SnapshotQuery query = dialect.buildBeforeQuery(target);

		assertThat(query.getSql()).isEqualTo("SELECT u.\"ID\", u.\"NAME\" FROM \"SYS_USER\" u "
				+ "JOIN department d ON d.id = ? WHERE u.id = ? FETCH FIRST 1001 ROWS ONLY FOR UPDATE");
		assertThat(query.getParameters()).extracting(parameter -> parameter.getParameterMapping().getProperty())
				.containsExactly("departmentId", "userId");
	}

	@Test
	void buildsParameterizedAfterQueryForCompositeIdentities() {
		ChangeTarget target = dialect.analyze(context("UPDATE sys_user SET name=? WHERE id=?", "name", "id"))
				.get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("TENANT_ID", "ID"), Arrays.asList("NAME"), 1000, false);
		List<RowIdentity> identities = Arrays.asList(
				identity("TENANT_ID", 7L, "ID", "secret-row-1"),
				identity("TENANT_ID", 8L, "ID", "secret-row-2"));

		SnapshotQuery query = dialect.buildAfterQuery(target, identities);

		assertThat(query.getSql()).isEqualTo("SELECT \"TENANT_ID\", \"ID\", \"NAME\" FROM \"SYS_USER\" "
				+ "WHERE (\"TENANT_ID\" = ? AND \"ID\" = ?) OR (\"TENANT_ID\" = ? AND \"ID\" = ?) "
				+ "FETCH FIRST 1001 ROWS ONLY");
		assertThat(query.getSql()).doesNotContain("secret-row-1", "secret-row-2");
		assertThat(query.getParameters()).extracting(SnapshotParameter::getValue)
				.containsExactly(7L, "secret-row-1", 8L, "secret-row-2");
	}

	@Test
	void preservesMergeUsingAndOnAsBeforeSnapshotJoin() {
		ChangeTarget target = dialect.analyze(context(
				"MERGE INTO sys_user target USING source_user source "
						+ "ON (target.id = source.id AND source.kind = ?) "
						+ "WHEN MATCHED THEN UPDATE SET target.name = ?",
				"kind", "name")).get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("ID"), Arrays.asList("NAME"), 1000, false);

		SnapshotQuery query = dialect.buildBeforeQuery(target);

		assertThat(query.getSql()).isEqualTo("SELECT target.\"ID\", target.\"NAME\" FROM \"SYS_USER\" target "
				+ "JOIN source_user source ON target.id = source.id AND source.kind = ? "
				+ "FETCH FIRST 1001 ROWS ONLY");
		assertThat(query.getParameters()).extracting(parameter -> parameter.getParameterMapping().getProperty())
				.containsExactly("kind");
	}

	@Test
	void deduplicatesSnapshotColumnsIgnoringCase() {
		ChangeTarget target = dialect.analyze(context("UPDATE sys_user SET name=? WHERE id=?", "name", "id"))
				.get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("ID"), Arrays.asList("id", "NAME"), 1000, false);

		SnapshotQuery query = dialect.buildAfterQuery(target,
				Arrays.asList(identity("ID", 1L, "IGNORED", 0L)));

		assertThat(query.getSql()).startsWith("SELECT \"ID\", \"NAME\" FROM \"SYS_USER\"");
	}

	@Test
	void executesJoinSnapshotWithQualifiedProjectionAndDerivedTableParameters() throws Exception {
		ChangeTarget target = dialect.analyze(context(
				"UPDATE sys_user u JOIN (SELECT CAST(? AS BIGINT) AS id) d ON d.id = u.id "
						+ "SET u.name = ? WHERE u.id = ?",
				"derivedId", "name", "userId")).get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("ID"), Arrays.asList("NAME"), 1000, false);
		SnapshotQuery query = dialect.buildBeforeQuery(target);

		assertThat(query.getSql()).isEqualTo("SELECT u.\"ID\", u.\"NAME\" FROM \"SYS_USER\" u "
				+ "JOIN (SELECT CAST(? AS BIGINT) AS id) d ON d.id = u.id "
				+ "WHERE u.id = ? FETCH FIRST 1001 ROWS ONLY");
		assertThat(query.getParameters()).extracting(parameter -> parameter.getParameterMapping().getProperty())
				.containsExactly("derivedId", "userId");

		try (Connection connection = h2("join_snapshot"); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE SYS_USER(ID BIGINT PRIMARY KEY, NAME VARCHAR(20))");
			statement.execute("INSERT INTO SYS_USER(ID, NAME) VALUES (1, 'before')");
			try (PreparedStatement prepared = connection.prepareStatement(query.getSql())) {
				prepared.setLong(1, 1L);
				prepared.setLong(2, 1L);
				try (ResultSet rows = prepared.executeQuery()) {
					assertThat(rows.next()).isTrue();
					assertThat(rows.getLong("ID")).isEqualTo(1L);
				}
			}
		}
	}

	@Test
	void executesMergeUsingSubselectWithAliasAndParametersInSqlOrder() throws Exception {
		ChangeTarget target = dialect.analyze(context(
				"MERGE INTO sys_user target USING (SELECT CAST(? AS BIGINT) AS id, "
						+ "CAST(? AS VARCHAR(10)) AS kind) source "
						+ "ON (target.id = source.id AND source.kind = ?) "
						+ "WHEN MATCHED THEN UPDATE SET target.name = ?",
				"usingId", "usingKind", "onKind", "name")).get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("ID"), Arrays.asList("NAME"), 1000, false);
		SnapshotQuery query = dialect.buildBeforeQuery(target);

		assertThat(query.getSql()).isEqualTo("SELECT target.\"ID\", target.\"NAME\" FROM \"SYS_USER\" target "
				+ "JOIN (SELECT CAST(? AS BIGINT) AS id, CAST(? AS VARCHAR (10)) AS kind) source "
				+ "ON target.id = source.id AND source.kind = ? FETCH FIRST 1001 ROWS ONLY");
		assertThat(query.getParameters()).extracting(parameter -> parameter.getParameterMapping().getProperty())
				.containsExactly("usingId", "usingKind", "onKind");

		try (Connection connection = h2("merge_snapshot"); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE SYS_USER(ID BIGINT PRIMARY KEY, NAME VARCHAR(20))");
			statement.execute("INSERT INTO SYS_USER(ID, NAME) VALUES (1, 'before')");
			try (PreparedStatement prepared = connection.prepareStatement(query.getSql())) {
				prepared.setLong(1, 1L);
				prepared.setString(2, "A");
				prepared.setString(3, "A");
				try (ResultSet rows = prepared.executeQuery()) {
					assertThat(rows.next()).isTrue();
				}
			}
		}
	}

	@Test
	void preservesQuotedIdentifierCaseInExecutableSnapshotSql() throws Exception {
		ChangeTarget target = dialect.analyze(context(
				"UPDATE \"mixedTable\" SET \"mixedName\" = ? WHERE \"mixedId\" = ?", "name", "id"))
				.get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("\"mixedId\""),
						Arrays.asList("\"mixedName\""), 1000, false);
		SnapshotQuery query = dialect.buildBeforeQuery(target);

		assertThat(query.getSql()).isEqualTo("SELECT \"mixedId\", \"mixedName\" FROM \"mixedTable\" "
				+ "WHERE \"mixedId\" = ? FETCH FIRST 1001 ROWS ONLY");
		try (Connection connection = h2("quoted_snapshot"); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE \"mixedTable\"(\"mixedId\" BIGINT PRIMARY KEY, \"mixedName\" VARCHAR(20))");
			try (PreparedStatement prepared = connection.prepareStatement(query.getSql())) {
				prepared.setLong(1, 1L);
				prepared.executeQuery();
			}
		}
	}

	@Test
	void directDialectEntryFailsClosedForReplace() {
		assertUnsupported("REPLACE INTO sys_user(id,name) VALUES (?,?)", "id", "name");
	}

	@Test
	void directDialectEntryFailsClosedForMultiRowInsert() {
		assertUnsupported("INSERT INTO sys_user(id,name) VALUES (?,?),(?,?)",
				"id1", "name1", "id2", "name2");
	}

	@Test
	void directDialectEntryFailsClosedForInsertSelect() {
		assertUnsupported("INSERT INTO sys_user(id,name) SELECT id,name FROM source_user");
	}

	private void assertUnsupported(String sql, String... mappings) {
		assertThatThrownBy(() -> dialect.analyze(context(sql, mappings)))
				.isInstanceOf(DataChangeException.class)
				.extracting(exception -> ((DataChangeException) exception).getCode())
				.isEqualTo(DataChangeErrorCode.UNSUPPORTED_SQL);
	}

	private BoundSqlContext context(String sql, String... properties) {
		Configuration configuration = new Configuration();
		List<ParameterMapping> mappings = new ArrayList<ParameterMapping>();
		for (String property : properties) {
			mappings.add(new ParameterMapping.Builder(configuration, property, Object.class).build());
		}
		return new BoundSqlContext(new BoundSql(configuration, sql, mappings, new Object()));
	}

	private Connection h2(String database) throws Exception {
		return DriverManager.getConnection("jdbc:h2:mem:" + database + ";DB_CLOSE_DELAY=-1", "sa", "");
	}

	private RowIdentity identity(String firstKey, Object firstValue, String secondKey, Object secondValue) {
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		keys.put(firstKey, firstValue);
		keys.put(secondKey, secondValue);
		return new RowIdentity(keys);
	}
}
