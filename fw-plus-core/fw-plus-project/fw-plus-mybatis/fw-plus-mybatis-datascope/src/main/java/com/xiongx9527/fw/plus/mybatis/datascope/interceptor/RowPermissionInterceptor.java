package com.xiongx9527.fw.plus.mybatis.datascope.interceptor;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLCommentHint;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLObjectImpl;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLExistsExpr;
import com.alibaba.druid.sql.ast.expr.SQLInSubQueryExpr;
import com.alibaba.druid.sql.ast.statement.SQLCallStatement;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLReplaceStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUnionQuery;
import com.alibaba.druid.sql.ast.statement.SQLUnionQueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.ast.statement.SQLWithSubqueryClause;
import com.xiongx9527.fw.plus.mybatis.datascope.adapter.DataScopeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datascope.adapter.DataScopeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datascope.configuration.RowPermission;
import com.xiongx9527.fw.plus.mybatis.datascope.consts.RowOperations;
import com.xiongx9527.fw.plus.mybatis.datascope.exception.RowPermissionConfigException;
import com.xiongx9527.fw.plus.mybatis.datascope.exception.RowPermissionHandleException;
import com.xiongx9527.fw.plus.mybatis.datascope.helper.RowPermissionHelper;
import com.xiongx9527.fw.plus.mybatis.datascope.provider.UserRowPermissionProvider;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Intercepts({@Signature(type = StatementHandler.class, method = "prepare",
		args = {Connection.class, Integer.class})})
public class RowPermissionInterceptor implements Interceptor {
	private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(.*?)}");
	private static final Pattern JOIN_HINT_PATTERN = Pattern.compile(
			"\\+.*\\s*(?<rp>RP\\(\\s*(left\\s*=\\s*(?<left>\\w+))?\\s*,?\\s*(right\\s*=\\s*(?<right>\\w+))?\\s*\\))\\s*.*");
	private static final Pattern EMPTY_JOIN_HINT_PATTERN = Pattern.compile("\\+.*\\s*");

	private final Logger logger = Logger.getLogger(getClass().getName());
	private final ExpressionParser spelExpressionParser = new SpelExpressionParser();
	private final Map<String, TableRowPermissionInternal> tablePermissionMap =
			new ConcurrentHashMap<String, TableRowPermissionInternal>();
	private final List<PatternTablePermission> tablePermissionRegexList =
			new ArrayList<PatternTablePermission>();
	private final Map<String, Expression> expressionCache = new ConcurrentHashMap<String, Expression>();
	private final Map<String, RowPermissionHolder> annotationCache =
			new ConcurrentHashMap<String, RowPermissionHolder>();
	private final UserRowPermissionProvider rowPermissionProvider;
	private final DataScopeFrameworkAdapterRegistry adapterRegistry;
	private volatile Integer scopeWhenMissingConfig;

	public RowPermissionInterceptor(UserRowPermissionProvider rowPermissionProvider) {
		this(rowPermissionProvider, null, null);
	}

	public RowPermissionInterceptor(UserRowPermissionProvider rowPermissionProvider,
			Integer scopeWhenMissingConfig) {
		this(rowPermissionProvider, scopeWhenMissingConfig, null);
	}

	public RowPermissionInterceptor(UserRowPermissionProvider rowPermissionProvider,
			Integer scopeWhenMissingConfig, DataScopeFrameworkAdapterRegistry adapterRegistry) {
		this.rowPermissionProvider = rowPermissionProvider;
		this.scopeWhenMissingConfig = scopeWhenMissingConfig;
		this.adapterRegistry = adapterRegistry == null
				? new DataScopeFrameworkAdapterRegistry(Collections.<DataScopeFrameworkAdapter>emptyList())
				: adapterRegistry;
		reload();
	}

	public final void setScopeWhenMissingConfig(Integer scopeWhenMissingConfig) {
		this.scopeWhenMissingConfig = scopeWhenMissingConfig;
	}

	public final void reload() {
		tablePermissionMap.clear();
		synchronized (tablePermissionRegexList) {
			tablePermissionRegexList.clear();
		}
		expressionCache.clear();
		annotationCache.clear();
		if (rowPermissionProvider == null) {
			return;
		}
		rowPermissionProvider.loadTableRowPermissionConfig(new com.xiongx9527.fw.plus.mybatis.datascope.provider
				.TableRowPermissionConfigConsumer() {
			@Override
			public void accept(String fileName, InputStream inputStream) {
				loadConfig(fileName, inputStream);
			}
		});
	}

	private void loadConfig(String fileName, InputStream inputStream) {
		if (inputStream == null) {
			throw RowPermissionConfigException.of(RowPermissionConfigException.INPUT_STREAM_EMPTY, fileName);
		}
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			Document document = factory.newDocumentBuilder().parse(inputStream);
			Element table = document.getDocumentElement();
			String tableName = attribute(table, "name");
			String dbType = childText(table, "db-type");
			if (isBlank(dbType)) {
				throw RowPermissionConfigException.of(RowPermissionConfigException.DB_TYPE_REQUIRED, fileName);
			}
			if (isBlank(tableName)) {
				throw RowPermissionConfigException.of(RowPermissionConfigException.TABLE_NAME_REQUIRED, fileName);
			}
			TableRowPermissionInternal internal = new TableRowPermissionInternal();
			internal.tableName = tableName.trim().toLowerCase();
			internal.tableAlias = childText(table, "table-alias");
			Element tenant = child(table, "tenant-id");
			internal.tenantIdColumn = tenant == null ? null : childText(tenant, "column");
			if (isRegex(internal.tableName)) {
				Pattern pattern = Pattern.compile(internal.tableName, Pattern.CASE_INSENSITIVE);
				synchronized (tablePermissionRegexList) {
					tablePermissionRegexList.add(new PatternTablePermission(pattern, internal));
				}
			} else {
				tablePermissionMap.put(internal.tableName, internal);
			}

			Element rowPermissions = child(table, "row-permissions");
			if (rowPermissions == null) {
				return;
			}
			Map<Integer, ScopeRowPermissionInternal> scopes =
					new LinkedHashMap<Integer, ScopeRowPermissionInternal>();
			for (Element rowPermission : children(rowPermissions, "row-permission")) {
				String scopeText = childText(rowPermission, "scope");
				if (isBlank(scopeText)) {
					throw RowPermissionConfigException.of(RowPermissionConfigException.SCOPE_REQUIRED, fileName);
				}
				Integer scope = Integer.valueOf(scopeText.trim());
				if (scopes.containsKey(scope)) {
					throw RowPermissionConfigException.of(RowPermissionConfigException.SCOPE_DUPLICATED, scope, fileName);
				}
				String clause = childText(rowPermission, "scope-clause");
				if (isBlank(clause)) {
					clause = null;
				} else {
					clause = clause.replace("{{TABLE_NAME}}", internal.tableName);
					cacheExpressions(internal.tableName, clause);
				}
				ScopeRowPermissionInternal scopePermission = new ScopeRowPermissionInternal();
				scopePermission.scopeClause = clause;
				scopes.put(scope, scopePermission);
			}
			internal.scopePermissionMap = scopes.isEmpty() ? null : scopes;
		} catch (RowPermissionConfigException exception) {
			throw exception;
		} catch (Exception exception) {
			throw RowPermissionConfigException.of(RowPermissionConfigException.LOAD_FAILED, fileName);
		}
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		if (rowPermissionProvider == null || !rowPermissionProvider.isRowPermissionEnabled()) {
			return invocation.proceed();
		}
		boolean clearThreadLocalStatus = false;
		try {
			StatementHandler statementHandler = (StatementHandler) realTarget(invocation.getTarget());
			MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
			MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
			BoundSql boundSql = (BoundSql) metaObject.getValue("delegate.boundSql");
			Object parameterObject = boundSql == null ? null : boundSql.getParameterObject();
			RowPermissionHelper.Status helperStatus = RowPermissionHelper.getStatus();
			int rowPermissionStatus;
			if (helperStatus == null) {
				DataScopeFrameworkAdapter adapter = adapterRegistry.select(mappedStatement, parameterObject);
				rowPermissionStatus = getRowPermissionStatus(mappedStatement, adapter);
			} else {
				rowPermissionStatus = helperStatus.getStatus();
				clearThreadLocalStatus = helperStatus.isAutoClearStatus();
			}
			if (rowPermissionStatus == RowPermission.STATUS_NEITHER || boundSql == null) {
				return invocation.proceed();
			}
			Connection connection = (Connection) invocation.getArgs()[0];
			DbType dbType = parseDbType(connection);
			List<SQLStatement> statements = SQLUtils.parseStatements(boundSql.getSql(), dbType);
			List<String> rewritten = new ArrayList<String>(statements.size());
			for (SQLStatement statement : statements) {
				rewritten.add(handleSingleStatement(statement, rowPermissionStatus));
			}
			metaObject.setValue("delegate.boundSql.sql", joinStatements(rewritten));
			return invocation.proceed();
		} finally {
			if (clearThreadLocalStatus) {
				RowPermissionHelper.clearStatus();
			}
		}
	}

	@Override
	public Object plugin(Object target) {
		return target instanceof StatementHandler ? Plugin.wrap(target, this) : target;
	}

	private String joinStatements(List<String> statements) {
		StringBuilder sql = new StringBuilder();
		for (String statement : statements) {
			if (sql.length() > 0) {
				sql.append(';');
			}
			sql.append(statement);
		}
		return sql.toString();
	}

	private DbType parseDbType(Connection connection) throws SQLException {
		String url = connection.getMetaData().getURL();
		int first = url == null ? -1 : url.indexOf(':');
		int second = first < 0 ? -1 : url.indexOf(':', first + 1);
		if (first < 0 || second < 0) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.DB_TYPE_UNRESOLVED, url);
		}
		try {
			DbType dbType = DbType.of(url.substring(first + 1, second));
			if (dbType == null) {
				throw new IllegalArgumentException("unknown db type");
			}
			return dbType;
		} catch (IllegalArgumentException exception) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_DB_TYPE, url);
		}
	}

	private String handleSingleStatement(SQLStatement statement, int rowPermissionStatus) {
		if (statement instanceof SQLSelectStatement) {
			handleSelectStatement((SQLSelectStatement) statement, rowPermissionStatus);
		} else if (statement instanceof SQLUpdateStatement) {
			handleUpdateStatement((SQLUpdateStatement) statement, rowPermissionStatus);
		} else if (statement instanceof SQLDeleteStatement) {
			handleDeleteStatement((SQLDeleteStatement) statement, rowPermissionStatus);
		} else if (statement instanceof SQLInsertStatement) {
			return SQLUtils.toSQLString(statement);
		} else if (statement instanceof SQLReplaceStatement || statement instanceof SQLCallStatement) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_SQL, statement);
		} else {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_SQL, statement);
		}
		return SQLUtils.toSQLString(statement);
	}

	private void handleSelectStatement(SQLSelectStatement statement, int rowPermissionStatus) {
		SQLSelect select = statement.getSelect();
		handleSelectQuery(select.getQuery(), rowPermissionStatus, rowPermissionProvider.getRequestPath());
	}

	private void handleSelectQuery(SQLSelectQuery query, int rowPermissionStatus, String requestPath) {
		if (query instanceof SQLSelectQueryBlock) {
			SQLSelectQueryBlock queryBlock = (SQLSelectQueryBlock) query;
			List<SQLExpr> extraWheres = new ArrayList<SQLExpr>();
			handleTableSource(extraWheres, queryBlock.getFrom(), Collections.<String>emptyList(),
					rowPermissionStatus, RowOperations.SELECT, requestPath);
			visitExpression(queryBlock.getWhere(), rowPermissionStatus, requestPath);
			for (SQLExpr expression : extraWheres) {
				queryBlock.addWhere(expression);
			}
			return;
		}
		if (query instanceof SQLUnionQuery) {
			SQLUnionQuery union = (SQLUnionQuery) query;
			handleSelectQuery(union.getLeft(), rowPermissionStatus, requestPath);
			handleSelectQuery(union.getRight(), rowPermissionStatus, requestPath);
			return;
		}
		throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_QUERY_TYPE, query.getClass().getName());
	}

	private void visitExpression(SQLExpr expression, int rowPermissionStatus, String requestPath) {
		if (expression instanceof SQLBinaryOpExpr) {
			SQLBinaryOpExpr binary = (SQLBinaryOpExpr) expression;
			visitExpression(binary.getLeft(), rowPermissionStatus, requestPath);
			visitExpression(binary.getRight(), rowPermissionStatus, requestPath);
		} else if (expression instanceof SQLInSubQueryExpr) {
			handleSelectQuery(((SQLInSubQueryExpr) expression).getSubQuery().getQuery(),
					rowPermissionStatus & RowPermission.STATUS_TENANT_ONLY, requestPath);
		} else if (expression instanceof SQLExistsExpr) {
			handleSelectQuery(((SQLExistsExpr) expression).getSubQuery().getQuery(),
					rowPermissionStatus & RowPermission.STATUS_TENANT_ONLY, requestPath);
		}
	}

	private void handleTableSource(List<SQLExpr> extraWheres, SQLTableSource tableSource,
			List<String> targetTables, int rowPermissionStatus, int operation, String requestPath) {
		if (tableSource == null) {
			return;
		}
		if (tableSource instanceof SQLExprTableSource) {
			SQLExprTableSource source = (SQLExprTableSource) tableSource;
			int actualOperation = targetTables.contains(source.computeAlias()) ? operation : RowOperations.SELECT;
			handleExprTableSource(extraWheres, source, rowPermissionStatus, actualOperation, requestPath);
		} else if (tableSource instanceof SQLJoinTableSource) {
			handleJoinTableSource(extraWheres, (SQLJoinTableSource) tableSource, targetTables,
					rowPermissionStatus, operation, requestPath);
		} else if (tableSource instanceof SQLSubqueryTableSource) {
			handleSelectQuery(((SQLSubqueryTableSource) tableSource).getSelect().getQuery(),
					rowPermissionStatus, requestPath);
		} else if (tableSource instanceof SQLUnionQueryTableSource) {
			for (SQLSelectQuery child : ((SQLUnionQueryTableSource) tableSource).getUnion().getChildren()) {
				handleSelectQuery(child, rowPermissionStatus, requestPath);
			}
		} else {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_TABLE_SOURCE_TYPE, tableSource.getClass().getName());
		}
	}

	private void handleExprTableSource(List<SQLExpr> extraWheres, SQLExprTableSource source,
			int rowPermissionStatus, int operation, String requestPath) {
		String tableName = source.getTableName().toLowerCase();
		String tableAlias = source.getAlias();
		if (isBlank(tableAlias) && operation != RowOperations.DELETE) {
			tableAlias = source.computeAlias();
			source.setAlias(tableAlias);
		}
		if (rowPermissionStatus == RowPermission.STATUS_NEITHER) {
			return;
		}
		TableRowPermissionInternal tableConfig = getTableRowPermissionConfig(tableName);
		if (!isBlank(tableConfig.tenantIdColumn)) {
			String prefix = isBlank(tableAlias) ? "" : tableAlias + ".";
			Long tenantId = rowPermissionProvider.getCurrentUserTenantId();
			extraWheres.add(SQLUtils.toSQLExpr(prefix + tableConfig.tenantIdColumn + "="
					+ (tenantId == null ? "NULL" : tenantId.toString())));
		}
		if (rowPermissionStatus != RowPermission.STATUS_TENANT_ONLY) {
			String clause = buildRowPermissionSql(tableConfig, operation, requestPath);
			if (!isBlank(clause)) {
				extraWheres.add(SQLUtils.toSQLExpr(
						replaceSqlPlaceholders(clause, tableConfig.tableAlias, tableAlias)));
			}
		}
	}

	private void handleJoinTableSource(List<SQLExpr> extraWheres, SQLJoinTableSource join,
			List<String> targetTables, int rowPermissionStatus, int operation, String requestPath) {
		List<Integer> hint = parseJoinRowPermissionHint(join.getCondition());
		int leftStatus = hint == null || hint.get(0) == null
				? rowPermissionStatus : hint.get(0) & rowPermissionStatus;
		int rightStatus = hint == null || hint.get(1) == null
				? rowPermissionStatus : hint.get(1) & rowPermissionStatus;
		List<SQLExpr> extraOn = new ArrayList<SQLExpr>(2);
		switch (join.getJoinType()) {
			case LEFT_OUTER_JOIN:
			case LEFT_SEMI_JOIN:
			case LEFT_ANTI_JOIN:
			case NATURAL_LEFT_JOIN:
				handleTableSource(extraWheres, join.getLeft(), targetTables, leftStatus, operation, requestPath);
				handleTableSource(extraOn, join.getRight(), targetTables, rightStatus, operation, requestPath);
				break;
			case RIGHT_OUTER_JOIN:
			case NATURAL_RIGHT_JOIN:
				handleTableSource(extraOn, join.getLeft(), targetTables, leftStatus, operation, requestPath);
				handleTableSource(extraWheres, join.getRight(), targetTables, rightStatus, operation, requestPath);
				break;
			case FULL_OUTER_JOIN:
			case OUTER_APPLY:
				handleTableSource(extraOn, join.getLeft(), targetTables, leftStatus, operation, requestPath);
				handleTableSource(extraOn, join.getRight(), targetTables, rightStatus, operation, requestPath);
				break;
			default:
				handleTableSource(extraWheres, join.getLeft(), targetTables, leftStatus, operation, requestPath);
				handleTableSource(extraWheres, join.getRight(), targetTables, rightStatus, operation, requestPath);
				break;
		}
		if (!extraOn.isEmpty()) {
			if (join.getUsing() != null && !join.getUsing().isEmpty()) {
				throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_JOIN_USING);
			}
			for (SQLExpr expression : extraOn) {
				join.addCondition(expression);
			}
		}
	}

	private void handleUpdateStatement(SQLUpdateStatement statement, int rowPermissionStatus) {
		if (statement.getFrom() != null) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_UPDATE_FROM);
		}
		SQLWithSubqueryClause with = statement.getWith();
		if (with != null) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_UPDATE_WITH);
		}
		List<String> targetTables = new ArrayList<String>();
		collectTargetTables(statement.getTableSource(), targetTables);
		List<SQLExpr> extraWheres = new ArrayList<SQLExpr>();
		handleTableSource(extraWheres, statement.getTableSource(), targetTables,
				rowPermissionStatus, RowOperations.UPDATE, null);
		for (SQLExpr expression : extraWheres) {
			statement.addWhere(expression);
		}
	}

	private void handleDeleteStatement(SQLDeleteStatement statement, int rowPermissionStatus) {
		if (statement.getWith() != null) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_DELETE_WITH);
		}
		List<String> targetTables = new ArrayList<String>();
		collectTargetTables(statement.getTableSource(), targetTables);
		List<SQLExpr> extraWheres = new ArrayList<SQLExpr>();
		if (statement.getFrom() != null) {
			handleTableSource(extraWheres, statement.getFrom(), targetTables,
					rowPermissionStatus, RowOperations.DELETE, null);
		}
		if (statement.getUsing() != null) {
			handleTableSource(extraWheres, statement.getUsing(), targetTables,
					rowPermissionStatus, RowOperations.DELETE, null);
		}
		if (statement.getFrom() == null && statement.getUsing() == null) {
			handleTableSource(extraWheres, statement.getTableSource(), targetTables,
					rowPermissionStatus, RowOperations.DELETE, null);
		}
		for (SQLExpr expression : extraWheres) {
			statement.addWhere(expression);
		}
	}

	private void collectTargetTables(SQLTableSource source, List<String> targetTables) {
		if (source instanceof SQLExprTableSource) {
			targetTables.add(((SQLExprTableSource) source).computeAlias());
		} else if (source instanceof SQLJoinTableSource) {
			collectTargetTables(((SQLJoinTableSource) source).getLeft(), targetTables);
			collectTargetTables(((SQLJoinTableSource) source).getRight(), targetTables);
		} else if (source instanceof SQLSubqueryTableSource) {
			return;
		} else {
			throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_TABLE_SOURCE_TYPE, source.getClass().getName());
		}
	}

	private TableRowPermissionInternal getTableRowPermissionConfig(String table) {
		TableRowPermissionInternal config = tablePermissionMap.get(table.toLowerCase());
		if (config != null) {
			return config;
		}
		synchronized (tablePermissionRegexList) {
			for (PatternTablePermission pattern : tablePermissionRegexList) {
				if (pattern.pattern.matcher(table).matches()) {
					return pattern.config;
				}
			}
		}
		throw RowPermissionHandleException.of(RowPermissionHandleException.TABLE_PERMISSION_UNDEFINED, table);
	}

	private String buildRowPermissionSql(TableRowPermissionInternal config, int operation, String requestPath) {
		if (!rowPermissionProvider.isSupportedOperation(operation)
				|| config.scopePermissionMap == null) {
			return null;
		}
		Set<Integer> scopes = rowPermissionProvider.getCurrentUserRowPermission(
				config.tableName, operation, requestPath);
		if (scopes == null || scopes.isEmpty()) {
			if (scopeWhenMissingConfig == null) {
				return "1=0";
			}
			scopes = Collections.singleton(scopeWhenMissingConfig);
		}
		StringBuilder sql = new StringBuilder();
		for (Integer scope : scopes) {
			ScopeRowPermissionInternal permission = config.scopePermissionMap.get(scope);
			if (permission == null) {
				throw RowPermissionHandleException.of(RowPermissionHandleException.SCOPE_PERMISSION_UNDEFINED,
						config.tableName, scope);
			}
			if (isBlank(permission.scopeClause)) {
				return null;
			}
			if (sql.length() == 0) {
				sql.append('(');
			} else {
				sql.append(" OR ");
			}
			sql.append('(').append(permission.scopeClause).append(')');
		}
		return sql.length() == 0 ? null : sql.append(')').toString();
	}

	private String replaceSqlPlaceholders(String sql, String formalAlias, String actualAlias) {
		String normalizedAlias = isBlank(actualAlias) ? "" : actualAlias + ".";
		if (!isBlank(formalAlias)) {
			sql = sql.replaceAll("\\b" + Pattern.quote(formalAlias) + "\\.", normalizedAlias);
		}
		Matcher matcher = PLACEHOLDER.matcher(sql);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			Object value = evaluateExpression(matcher.group(1));
			matcher.appendReplacement(result, Matcher.quoteReplacement(toSqlLiteral(value)));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private Object evaluateExpression(String expressionText) {
		Expression expression = expressionCache.get(expressionText);
		if (expression == null) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.EXPRESSION_NOT_PRECOMPILED, expressionText);
		}
		EvaluationContext context = new StandardEvaluationContext();
		return expression.getValue(context, rowPermissionProvider);
	}

	private String toSqlLiteral(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof Collection) {
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty()) {
				return "(NULL)";
			}
			StringBuilder result = new StringBuilder("(");
			for (Object item : collection) {
				if (result.length() > 1) {
					result.append(',');
				}
				result.append(toSqlScalar(item));
			}
			return result.append(')').toString();
		}
		return toSqlScalar(value);
	}

	private String toSqlScalar(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof String) {
			return "'" + ((String) value).replace("'", "''") + "'";
		}
		if (value instanceof Integer || value instanceof Long || value instanceof Boolean) {
			return value.toString();
		}
		throw RowPermissionHandleException.of(RowPermissionHandleException.UNSUPPORTED_VALUE_TYPE, value.getClass().getName());
	}

	private int getRowPermissionStatus(MappedStatement mappedStatement,
			DataScopeFrameworkAdapter adapter) {
		RowPermission annotation = getRowPermissionAnnotation(mappedStatement, adapter);
		if (annotation == null) {
			return RowPermission.STATUS_BOTH;
		}
		if (annotation.status() != RowPermission.STATUS_ON_CONDITION) {
			return validateStatus(annotation.status());
		}
		Expression expression = expressionCache.get(annotation.condition());
		if (expression == null) {
			try {
				expression = spelExpressionParser.parseExpression(annotation.condition());
				expressionCache.put(annotation.condition(), expression);
			} catch (SpelParseException exception) {
				throw RowPermissionHandleException.of(RowPermissionHandleException.CONDITION_PARSE_FAILED, annotation.condition());
			}
		}
		Object value = expression.getValue(new StandardEvaluationContext(), rowPermissionProvider);
		if (!(value instanceof Number)) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.CONDITION_NOT_NUMERIC, annotation.condition());
		}
		return validateStatus(((Number) value).intValue());
	}

	private int validateStatus(int status) {
		if (status != RowPermission.STATUS_NEITHER && status != RowPermission.STATUS_TENANT_ONLY
				&& status != RowPermission.STATUS_BOTH) {
			throw RowPermissionHandleException.of(RowPermissionHandleException.INVALID_PERMISSION_STATUS, status);
		}
		return status;
	}

	private RowPermission getRowPermissionAnnotation(MappedStatement mappedStatement,
			DataScopeFrameworkAdapter adapter) {
		if (mappedStatement == null || adapter == null) {
			return null;
		}
		String statementId = mappedStatement.getId();
		RowPermissionHolder cached = annotationCache.get(statementId);
		if (cached != null) {
			return cached.annotation;
		}
		Class<?> mapperType = adapter.resolveMapperType(mappedStatement);
		if (mapperType == null) {
			annotationCache.put(statementId, RowPermissionHolder.EMPTY);
			return null;
		}
		String methodName = statementId.substring(statementId.lastIndexOf('.') + 1);
		java.lang.reflect.Method selected = null;
		for (java.lang.reflect.Method method : mapperType.getMethods()) {
			if (method.getName().equals(methodName)) {
				selected = method;
				if (method.getAnnotation(RowPermission.class) != null) {
					break;
				}
			}
		}
		RowPermission annotation = selected == null ? null : selected.getAnnotation(RowPermission.class);
		if (annotation == null) {
			annotation = mapperType.getAnnotation(RowPermission.class);
		}
		annotationCache.put(statementId, new RowPermissionHolder(annotation));
		return annotation;
	}

	private List<Integer> parseJoinRowPermissionHint(SQLExpr condition) {
		if (condition == null || condition.getHint() == null) {
			return null;
		}
		SQLCommentHint hint = condition.getHint();
		Matcher matcher = JOIN_HINT_PATTERN.matcher(hint.getText());
		if (!matcher.matches()) {
			return null;
		}
		List<Integer> result = new ArrayList<Integer>(2);
		Integer left = parseRowPermissionText(matcher.group("left"));
		Integer right = parseRowPermissionText(matcher.group("right"));
		if ((matcher.group("left") != null && left == null)
				|| (matcher.group("right") != null && right == null)) {
			return null;
		}
		result.add(left);
		result.add(right);
		String remain = hint.getText().substring(0, matcher.start("rp"))
				+ hint.getText().substring(matcher.end("rp"));
		if (EMPTY_JOIN_HINT_PATTERN.matcher(remain).matches()) {
			if (condition instanceof SQLObjectImpl) {
				((SQLObjectImpl) condition).setHint(null);
			} else {
				hint.setText("");
			}
		} else {
			hint.setText(remain);
		}
		return result;
	}

	private Integer parseRowPermissionText(String text) {
		if (text == null) {
			return null;
		}
		String normalized = text.toLowerCase();
		if ("tenant".equals(normalized) || "tenant_only".equals(normalized)) {
			return RowPermission.STATUS_TENANT_ONLY;
		}
		if ("both".equals(normalized)) {
			return RowPermission.STATUS_BOTH;
		}
		if ("none".equals(normalized) || "neither".equals(normalized)) {
			return RowPermission.STATUS_NEITHER;
		}
		logger.warning("无效的RowPermissionHint: " + text);
		return null;
	}

	private void cacheExpressions(String tableName, String clause) {
		Matcher matcher = PLACEHOLDER.matcher(clause);
		while (matcher.find()) {
			String expressionText = matcher.group(1);
			try {
				expressionCache.put(expressionText, spelExpressionParser.parseExpression(expressionText));
			} catch (SpelParseException exception) {
				throw RowPermissionConfigException.of(RowPermissionConfigException.SCOPE_CLAUSE_PARSE_FAILED,
						tableName, clause);
			}
		}
	}

	private Object realTarget(Object target) {
		Object current = target;
		while (current != null && Proxy.isProxyClass(current.getClass())) {
			InvocationHandler handler = Proxy.getInvocationHandler(current);
			MetaObject metaObject = SystemMetaObject.forObject(handler);
			if (!metaObject.hasGetter("target")) {
				return current;
			}
			Object next = metaObject.getValue("target");
			if (next == null || next == current) {
				return current;
			}
			current = next;
		}
		return current;
	}

	private boolean isRegex(String tableName) {
		return tableName.indexOf('.') >= 0 || tableName.indexOf('*') >= 0 || tableName.indexOf('?') >= 0
				|| tableName.indexOf('[') >= 0 || tableName.indexOf(']') >= 0
				|| tableName.indexOf('{') >= 0 || tableName.indexOf('}') >= 0
				|| tableName.indexOf('+') >= 0 || tableName.indexOf('^') >= 0
				|| tableName.indexOf('$') >= 0;
	}

	private Element child(Element parent, String name) {
		NodeList nodes = parent.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element && name.equals(node.getNodeName())) {
				return (Element) node;
			}
		}
		return null;
	}

	private List<Element> children(Element parent, String name) {
		List<Element> result = new ArrayList<Element>();
		NodeList nodes = parent.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node instanceof Element && name.equals(node.getNodeName())) {
				result.add((Element) node);
			}
		}
		return result;
	}

	private String childText(Element parent, String name) {
		Element child = child(parent, name);
		return child == null ? null : child.getTextContent().trim();
	}

	private String attribute(Element element, String name) {
		return element == null || !element.hasAttribute(name) ? null : element.getAttribute(name);
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private static final class RowPermissionHolder {
		private static final RowPermissionHolder EMPTY = new RowPermissionHolder(null);
		private final RowPermission annotation;

		private RowPermissionHolder(RowPermission annotation) {
			this.annotation = annotation;
		}
	}

	private static final class PatternTablePermission {
		private final Pattern pattern;
		private final TableRowPermissionInternal config;

		private PatternTablePermission(Pattern pattern, TableRowPermissionInternal config) {
			this.pattern = pattern;
			this.config = config;
		}
	}

	private static final class TableRowPermissionInternal {
		private String tableName;
		private String tableAlias;
		private String tenantIdColumn;
		private Map<Integer, ScopeRowPermissionInternal> scopePermissionMap;
	}

	private static final class ScopeRowPermissionInternal {
		private String scopeClause;
	}
}
