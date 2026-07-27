package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataSqlPipelineTest {

	@Test
	public void shouldOrderPluginsAndAuditEveryPhase() {
		AskDataSqlAudit audit = new AskDataSqlAudit();
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Arrays.asList(
				interceptor("later", 20, " /* later */"), interceptor("first", 10, " /* first */")),
				new PermissiveValidator(), audit);

		AskDataSqlCommand result = chain.execute(command("select * from sales"));

		assertEquals("select * from sales /* first */ /* later */", result.getSql());
		List<AskDataSqlAudit.Record> records = audit.getRecords();
		assertEquals("first", records.get(0).getInterceptorKey());
		assertEquals("INTERCEPT", records.get(0).getPhase());
		assertEquals("later", records.get(1).getInterceptorKey());
		assertEquals("FINAL_VALIDATE", records.get(2).getPhase());
	}

	@Test
	public void shouldKeepNamedParametersByName() {
		AskDataSqlCommand command = new AskDataSqlCommand(new AskDataProjectContext("project-a", "operator-a"),
				"select * from sales where tenant_id = :tenantId", Arrays.asList(
						AskDataSqlParameter.of("tenantId", "tenant-a")), Collections.singleton("sales"));

		assertEquals("tenant-a", command.getParameter("tenantId").getValue());
		assertEquals("tenantId", command.getParameters().get(0).getName());
	}

	@Test
	public void shouldRejectMultipleStatements() {
		assertRejected("select * from sales; select * from users", AskDataSqlValidator.FailureCategory.MULTI_STATEMENT);
	}

	@Test
	public void shouldRejectWriteSql() {
		assertRejected("delete from sales", AskDataSqlValidator.FailureCategory.WRITE_SQL);
	}

	@Test
	public void shouldRejectUnauthorizedObject() {
		assertRejected("select * from users", AskDataSqlValidator.FailureCategory.UNAUTHORIZED_OBJECT);
	}

	@Test
	public void shouldValidateAgainAfterPluginRewrite() {
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				interceptor("rewrite", 1, "; select * from users")), new SafetyValidator(), new AskDataSqlAudit());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> chain.execute(command("select * from sales")));

		assertTrue(exception.getMessage().contains("MULTI_STATEMENT"));
	}

	private static void assertRejected(String sql, AskDataSqlValidator.FailureCategory category) {
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.<AskDataSqlInterceptor>emptyList(),
				new SafetyValidator(), new AskDataSqlAudit());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> chain.execute(command(sql)));

		assertTrue(exception.getMessage().contains(category.name()));
	}

	private static AskDataSqlCommand command(String sql) {
		return new AskDataSqlCommand(new AskDataProjectContext("project-a", "operator-a"), sql,
				Collections.<AskDataSqlParameter>emptyList(), Collections.singleton("sales"));
	}

	private static AskDataSqlInterceptor interceptor(final String key, final int order, final String suffix) {
		return new AskDataSqlInterceptor() {
			@Override
			public String getKey() {
				return key;
			}

			@Override
			public int getOrder() {
				return order;
			}

			@Override
			public AskDataSqlCommand intercept(AskDataSqlCommand command) {
				return command.withSql(command.getSql() + suffix);
			}
		};
	}

	private static class PermissiveValidator implements AskDataSqlValidator {
		@Override
		public Result validate(AskDataSqlCommand command) {
			return Result.success();
		}
	}

	private static class SafetyValidator implements AskDataSqlValidator {
		@Override
		public Result validate(AskDataSqlCommand command) {
			String normalizedSql = command.getSql().trim().toLowerCase();
			if (normalizedSql.indexOf(';') >= 0) {
				return Result.failure(FailureCategory.MULTI_STATEMENT, "SQL 只允许单条语句");
			}
			if (normalizedSql.startsWith("insert") || normalizedSql.startsWith("update")
					|| normalizedSql.startsWith("delete") || normalizedSql.startsWith("merge")
					|| normalizedSql.startsWith("create") || normalizedSql.startsWith("drop")
					|| normalizedSql.startsWith("alter")) {
				return Result.failure(FailureCategory.WRITE_SQL, "SQL 仅允许只读查询");
			}
			Set<String> objects = referencedObjects(normalizedSql);
			for (String object : objects) {
				if (!command.getAuthorizedObjects().contains(object)) {
					return Result.failure(FailureCategory.UNAUTHORIZED_OBJECT, "对象未授权: " + object);
				}
			}
			return Result.success();
		}

		private Set<String> referencedObjects(String sql) {
			Set<String> objects = new HashSet<String>();
			String[] tokens = sql.split("\\s+");
			for (int index = 0; index < tokens.length - 1; index++) {
				if ("from".equals(tokens[index])) {
					objects.add(tokens[index + 1]);
				}
			}
			return objects;
		}
	}
}
