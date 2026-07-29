package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataSqlPipelineTest {

	@Test
	public void shouldCarryAuthorizationSummaryWhenSqlChanges() {
		AskDataAuthorizationSummary summary = authorizationSummary();
		AskDataSqlCommand command = commandWithAuthorizationSummary(summary);

		AskDataSqlCommand rewritten = command.withSql("select * from sales where tenant_id = :tenantId");

		assertSame(summary, rewritten.getAuthorizationSummary());
	}

	@Test
	public void shouldRejectPluginThatExpandsAuthorizationSummaryObjects() {
		AskDataAuthorizationSummary originalSummary = authorizationSummary();
		AskDataAuthorizationSummary expandedSummary = AskDataAuthorizationSummary.allow(Arrays.asList(
				new AskDataAuthorizedObject("analytics", "sales", "tenant_id"),
				new AskDataAuthorizedObject("analytics", "users", "id")), originalSummary.getRowPredicates());
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				interceptorReplacingAuthorizationSummary(expandedSummary)), new PermissiveValidator(), new AskDataSqlAudit());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> chain.execute(commandWithAuthorizationSummary(originalSummary)));

		assertTrue(exception.getMessage().contains("authorizationSummary"));
	}

	@Test
	public void shouldRejectPluginThatClearsAuthorizationSummaryObjectsWithoutRowPredicates() {
		AskDataAuthorizationSummary originalSummary = authorizationSummaryWithoutRowPredicates();
		AskDataAuthorizationSummary clearedSummary = AskDataAuthorizationSummary.allow(
				Collections.<AskDataAuthorizedObject>emptyList(), Collections.<AskDataRowPredicate>emptyList());
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				interceptorReplacingAuthorizationSummary(clearedSummary)), new PermissiveValidator(), new AskDataSqlAudit());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> chain.execute(commandWithAuthorizationSummary(originalSummary)));

		assertTrue(exception.getMessage().contains("authorizationSummary"));
	}

	@Test
	public void shouldRejectPluginThatRemovesRequiredRowPredicate() {
		AskDataAuthorizationSummary originalSummary = authorizationSummary();
		AskDataAuthorizationSummary weakenedSummary = AskDataAuthorizationSummary.allow(
				originalSummary.getAuthorizedObjects(), Collections.<AskDataRowPredicate>emptyList());
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				interceptorReplacingAuthorizationSummary(weakenedSummary)), new PermissiveValidator(), new AskDataSqlAudit());

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> chain.execute(commandWithAuthorizationSummary(originalSummary)));

		assertTrue(exception.getMessage().contains("authorizationSummary"));
	}

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
	public void shouldAuditInterceptorWithConstructionTimeKeyWhenKeyChangesDuringExecution() {
		AskDataSqlAudit audit = new AskDataSqlAudit();
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				new AskDataSqlInterceptor() {
					private int keyRequests;

						@Override
						public String getKey() {
							return keyRequests++ == 0 ? "stable" : "validator";
						}

					@Override
					public int getOrder() {
						return 1;
					}

					@Override
					public AskDataSqlCommand intercept(AskDataSqlCommand command) {
						return command;
					}
				}), new PermissiveValidator(), audit);

		chain.execute(command("select * from sales"));

		assertEquals("stable", audit.getRecords().get(0).getInterceptorKey());
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
	public void shouldSnapshotArrayParameterValue() {
		int[] source = new int[] { 1, 2 };
		AskDataSqlParameter parameter = AskDataSqlParameter.of("ids", source);

		source[0] = 9;
		int[] exposed = (int[]) parameter.getValue();
		exposed[1] = 8;

		assertEquals(1, ((int[]) parameter.getValue())[0]);
		assertEquals(2, ((int[]) parameter.getValue())[1]);
	}

	@Test
	public void shouldSnapshotCollectionParameterValue() {
		List<String> source = new ArrayList<String>();
		source.add("tenant-a");
		AskDataSqlParameter parameter = AskDataSqlParameter.of("tenants", source);

		source.add("tenant-b");
		List<?> exposed = (List<?>) parameter.getValue();

		assertEquals(Collections.singletonList("tenant-a"), exposed);
		assertThrows(UnsupportedOperationException.class, () -> exposed.clear());
	}

	@Test
	public void shouldSnapshotDateParameterValue() {
		Date source = new Date(1000L);
		AskDataSqlParameter parameter = AskDataSqlParameter.of("createdAt", source);

		source.setTime(2000L);
		Date exposed = (Date) parameter.getValue();
		exposed.setTime(3000L);

		assertEquals(1000L, ((Date) parameter.getValue()).getTime());
	}

	@Test
	public void shouldDeepSnapshotNestedParameterValue() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(1000L);
		List<Object> nestedList = new ArrayList<Object>();
		nestedList.add(calendar);
		Map<String, Object> source = new LinkedHashMap<String, Object>();
		source.put("items", nestedList);

		AskDataSqlParameter parameter = AskDataSqlParameter.of("payload", source);

		calendar.setTimeInMillis(2000L);
		nestedList.add("changed");
		Map<?, ?> exposed = (Map<?, ?>) parameter.getValue();
		List<?> exposedList = (List<?>) exposed.get("items");
		((Calendar) exposedList.get(0)).setTimeInMillis(3000L);

		Map<?, ?> snapshot = (Map<?, ?>) parameter.getValue();
		List<?> snapshotList = (List<?>) snapshot.get("items");
		assertEquals(1, snapshotList.size());
		assertEquals(1000L, ((Calendar) snapshotList.get(0)).getTimeInMillis());
		assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
		assertThrows(UnsupportedOperationException.class, () -> snapshotList.clear());
	}

	@Test
	public void shouldRejectParameterValueThatCannotBeSafelySnapshotted() {
		assertThrows(IllegalArgumentException.class, () -> AskDataSqlParameter.of("unsafe", new StringBuilder("x")));
	}

	@Test
	public void shouldRetainOnlyBoundedNumberOfAuditRecords() {
		AskDataSqlAudit audit = new AskDataSqlAudit();
		for (int index = 0; index < 1025; index++) {
			audit.record("plugin-" + index, "INTERCEPT");
		}

		assertEquals(1024, audit.getRecords().size());
		assertEquals("plugin-1", audit.getRecords().get(0).getInterceptorKey());
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

	@Test
	public void shouldFinalValidateWithOriginalSecurityBoundaryWhenPluginReplacesCommand() {
		AskDataSqlAudit audit = new AskDataSqlAudit();
		AskDataSqlCommand original = new AskDataSqlCommand(new AskDataProjectContext("project-a", "operator-a"),
				"select * from sales where tenant_id = :tenantId",
				Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")), Collections.singleton("sales"));
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				maliciousSqlInterceptor("combined", new AskDataProjectContext("project-b", "operator-b"),
						Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-b")),
						Collections.singleton("users"), "select * from users where tenant_id = :tenantId")),
				new SafetyValidator(), audit);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> chain.execute(original));

		assertTrue(exception.getMessage().contains("UNAUTHORIZED_OBJECT"));
		assertEquals("combined", audit.getRecords().get(0).getInterceptorKey());
		assertEquals("FINAL_VALIDATE", audit.getRecords().get(1).getPhase());
	}

	@Test
	public void shouldPreserveAuthorizedObjectsWhenPluginAttemptsToExpandAuthorization() {
		AskDataSqlCommand original = command("select * from sales");
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				maliciousInterceptor("authorized-objects", new AskDataProjectContext("project-a", "operator-a"),
						Collections.<AskDataSqlParameter>emptyList(), new HashSet<String>(Arrays.asList("sales", "users")))),
				new PermissiveValidator(), new AskDataSqlAudit());

		AskDataSqlCommand result = chain.execute(original);

		assertEquals(Collections.singleton("sales"), result.getAuthorizedObjects());
	}

	@Test
	public void shouldPreserveParametersWhenPluginAttemptsToReplaceBindings() {
		List<AskDataSqlParameter> originalParameters = Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a"));
		AskDataSqlCommand original = new AskDataSqlCommand(new AskDataProjectContext("project-a", "operator-a"),
				"select * from sales where tenant_id = :tenantId", originalParameters, Collections.singleton("sales"));
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				maliciousInterceptor("parameters", new AskDataProjectContext("project-a", "operator-a"),
						Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-b")), Collections.singleton("sales"))),
				new PermissiveValidator(), new AskDataSqlAudit());

		AskDataSqlCommand result = chain.execute(original);

		assertEquals("tenant-a", result.getParameter("tenantId").getValue());
	}

	@Test
	public void shouldPreserveContextWhenPluginAttemptsToReplaceIsolationBoundary() {
		AskDataSqlCommand original = command("select * from sales");
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Collections.singletonList(
				maliciousInterceptor("context", new AskDataProjectContext("project-b", "operator-b"),
						Collections.<AskDataSqlParameter>emptyList(), Collections.singleton("sales"))),
				new PermissiveValidator(), new AskDataSqlAudit());

		AskDataSqlCommand result = chain.execute(original);

		assertSame(original.getContext(), result.getContext());
	}

	@Test
	public void shouldOrderPluginsWithExtremeIntegerOrders() {
		AskDataSqlInterceptorChain chain = new AskDataSqlInterceptorChain(Arrays.asList(
				interceptor("last", Integer.MAX_VALUE, " /* last */"),
				interceptor("first", Integer.MIN_VALUE, " /* first */")), new PermissiveValidator(), new AskDataSqlAudit());

		AskDataSqlCommand result = chain.execute(command("select * from sales"));

		assertEquals("select * from sales /* first */ /* last */", result.getSql());
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

	private static AskDataSqlCommand commandWithAuthorizationSummary(AskDataAuthorizationSummary summary) {
		return new AskDataSqlCommand(new AskDataProjectContext("project-a", "operator-a"),
				"select * from sales where tenant_id = :tenantId",
				Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")),
				Collections.singleton("sales"), summary);
	}

	private static AskDataAuthorizationSummary authorizationSummary() {
		AskDataAuthorizedObject object = new AskDataAuthorizedObject("analytics", "sales", "tenant_id");
		AskDataRowPredicate predicate = new AskDataRowPredicate(object, "tenant_id = :tenantId",
				Collections.singletonList(AskDataSqlParameter.of("tenantId", "tenant-a")));
		return AskDataAuthorizationSummary.allow(Collections.singletonList(object), Collections.singletonList(predicate));
	}

	private static AskDataAuthorizationSummary authorizationSummaryWithoutRowPredicates() {
		return AskDataAuthorizationSummary.allow(Collections.singletonList(
				new AskDataAuthorizedObject("analytics", "sales", "tenant_id")),
				Collections.<AskDataRowPredicate>emptyList());
	}

	private static AskDataSqlInterceptor interceptorReplacingAuthorizationSummary(
			final AskDataAuthorizationSummary authorizationSummary) {
		return new AskDataSqlInterceptor() {
			@Override
			public String getKey() {
				return "authorization-summary";
			}

			@Override
			public int getOrder() {
				return 1;
			}

			@Override
			public AskDataSqlCommand intercept(AskDataSqlCommand command) {
				return command.withAuthorizationSummary(authorizationSummary);
			}
		};
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

	private static AskDataSqlInterceptor maliciousInterceptor(final String key, final AskDataProjectContext context,
			List<AskDataSqlParameter> parameters, Set<String> authorizedObjects) {
		return maliciousSqlInterceptor(key, context, parameters, authorizedObjects, null);
	}

	private static AskDataSqlInterceptor maliciousSqlInterceptor(final String key, final AskDataProjectContext context,
			List<AskDataSqlParameter> parameters, Set<String> authorizedObjects, final String sql) {
		final List<AskDataSqlParameter> maliciousParameters = parameters;
		final Set<String> maliciousAuthorizedObjects = authorizedObjects;
		return new AskDataSqlInterceptor() {
			@Override
			public String getKey() {
				return key;
			}

			@Override
			public int getOrder() {
				return 1;
			}

			@Override
			public AskDataSqlCommand intercept(AskDataSqlCommand command) {
				String rewrittenSql = sql == null ? command.getSql() : sql;
				return new AskDataSqlCommand(context, rewrittenSql, maliciousParameters, maliciousAuthorizedObjects);
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
