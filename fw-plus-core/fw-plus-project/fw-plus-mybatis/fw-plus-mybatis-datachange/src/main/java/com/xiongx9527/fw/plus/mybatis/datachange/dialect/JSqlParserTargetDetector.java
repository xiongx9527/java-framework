package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserConstants;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.values.ValuesStatement;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.RowConstructor;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class JSqlParserTargetDetector {

	public SqlTargetDetection detect(String sql) {
		List<SqlTargetDetection.StatementDetection> detections =
				new ArrayList<SqlTargetDetection.StatementDetection>();
		for (ParsedStatement parsed : parseSegments(sql)) {
			if (parsed.isCall() || parsed.getStatement() instanceof Execute) {
				detections.add(nonAuditable(parsed.getStatementIndex()));
				continue;
			}
			StatementOperation operation = operation(parsed.getStatement());
			if (operation == null) {
				if (isDmlToken(parsed.getFirstTokenKind())) {
					throw unsupported();
				}
				detections.add(nonAuditable(parsed.getStatementIndex()));
				continue;
			}
			if (parsed.getStatement() instanceof Insert && !isSupportedInsert((Insert) parsed.getStatement())) {
				throw unsupported();
			}
			List<Table> targetTables = targetTables(parsed.getStatement());
			if (targetTables.isEmpty()) {
				throw unsupported();
			}
			List<TableIdentifier> identifiers = new ArrayList<TableIdentifier>();
			for (Table table : targetTables) {
				TableIdentifier identifier = identifier(table);
				if (identifier.getTable() == null || identifier.getTable().isEmpty()) {
					throw unsupported();
				}
				identifiers.add(identifier);
			}
			detections.add(new SqlTargetDetection.StatementDetection(parsed.getStatementIndex(), true, true,
					operation, identifiers));
		}
		return new SqlTargetDetection(detections);
	}

	List<ParsedStatement> parseSegments(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			return Collections.emptyList();
		}
		try {
			return tokenize(sql);
		} catch (RuntimeException exception) {
			throw new SqlAnalysisException("Unable to parse SQL statement", exception);
		}
	}

	private List<ParsedStatement> tokenize(String sql) {
		CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
		List<Integer> lineStarts = lineStarts(sql);
		List<ParsedStatement> parsed = new ArrayList<ParsedStatement>();
		int segmentStart = 0;
		int firstTokenKind = -1;
		int parameterCount = 0;
		int statementIndex = 0;
		while (true) {
			Token token = parser.getNextToken();
			if (token.kind == CCJSqlParserConstants.EOF) {
				if (firstTokenKind >= 0) {
					parsed.add(parseSegment(sql.substring(segmentStart), statementIndex,
							firstTokenKind, parameterCount));
				}
				return parsed;
			}
			if (token.kind == CCJSqlParserConstants.ST_SEMICOLON) {
				if (firstTokenKind >= 0) {
					int end = offset(lineStarts, token.beginLine, token.beginColumn);
					parsed.add(parseSegment(sql.substring(segmentStart, end), statementIndex++,
							firstTokenKind, parameterCount));
				}
				segmentStart = offset(lineStarts, token.endLine, token.endColumn) + 1;
				firstTokenKind = -1;
				parameterCount = 0;
				continue;
			}
			if (firstTokenKind < 0) {
				firstTokenKind = token.kind;
			}
			if ("?".equals(token.image)) {
				parameterCount++;
			}
		}
	}

	private ParsedStatement parseSegment(String segment, int statementIndex, int firstTokenKind,
											 int parameterCount) {
		if (firstTokenKind == CCJSqlParserConstants.K_CALL) {
			return new ParsedStatement(statementIndex, firstTokenKind, parameterCount, true, null);
		}
		try {
			Statements statements = CCJSqlParserUtil.parseStatements(segment);
			if (statements.getStatements().size() != 1) {
				throw unsupported();
			}
			return new ParsedStatement(statementIndex, firstTokenKind, parameterCount, false,
					statements.getStatements().get(0));
		} catch (JSQLParserException exception) {
			throw new SqlAnalysisException("Unable to parse SQL statement", exception);
		}
	}

	private List<Integer> lineStarts(String sql) {
		List<Integer> starts = new ArrayList<Integer>();
		starts.add(0);
		for (int index = 0; index < sql.length(); index++) {
			if (sql.charAt(index) == '\n') {
				starts.add(index + 1);
			}
		}
		return starts;
	}

	private int offset(List<Integer> lineStarts, int line, int column) {
		return lineStarts.get(line - 1) + column - 1;
	}

	private SqlTargetDetection.StatementDetection nonAuditable(int statementIndex) {
		return new SqlTargetDetection.StatementDetection(statementIndex, false, true, null,
				Collections.<TableIdentifier>emptyList());
	}

	static StatementOperation operation(Statement statement) {
		if (statement instanceof Insert) { return StatementOperation.INSERT; }
		if (statement instanceof Update) { return StatementOperation.UPDATE; }
		if (statement instanceof Delete) { return StatementOperation.PHYSICAL_DELETE; }
		if (statement instanceof Merge) { return StatementOperation.MERGE; }
		return null;
	}

	static List<Table> targetTables(Statement statement) {
		if (statement instanceof Insert) {
			return singleton(((Insert) statement).getTable());
		}
		if (statement instanceof Update) {
			return singleton(((Update) statement).getTable());
		}
		if (statement instanceof Merge) {
			return singleton(((Merge) statement).getTable());
		}
		if (statement instanceof Delete) {
			return deleteTargets((Delete) statement);
		}
		return Collections.emptyList();
	}

	private static List<Table> singleton(Table table) {
		return table == null ? Collections.<Table>emptyList() : Collections.singletonList(table);
	}

	private static List<Table> deleteTargets(Delete delete) {
		if (delete.getTables() == null || delete.getTables().isEmpty()) {
			return singleton(delete.getTable());
		}
		Map<String, Table> sources = new LinkedHashMap<String, Table>();
		addSource(sources, delete.getTable());
		if (delete.getJoins() != null) {
			for (Join join : delete.getJoins()) {
				if (join.getRightItem() instanceof Table) {
					addSource(sources, (Table) join.getRightItem());
				}
			}
		}
		List<Table> resolved = new ArrayList<Table>();
		for (Table target : delete.getTables()) {
			Table source = sources.get(normalized(target.getName()));
			if (source == null) {
				return Collections.emptyList();
			}
			resolved.add(source);
		}
		return resolved;
	}

	private static void addSource(Map<String, Table> sources, Table table) {
		if (table == null) { return; }
		sources.put(normalized(table.getName()), table);
		if (table.getAlias() != null) {
			sources.put(normalized(table.getAlias().getName()), table);
		}
	}

	static TableIdentifier identifier(Table table) {
		List<String> parts = table.getNameParts();
		if (parts.size() >= 3) {
			return new TableIdentifier(unquote(parts.get(2)), unquote(parts.get(1)), unquote(parts.get(0)));
		}
		if (parts.size() == 2) {
			return new TableIdentifier(null, unquote(parts.get(1)), unquote(parts.get(0)));
		}
		return new TableIdentifier(null, null, unquote(table.getName()));
	}

	private static String normalized(String value) {
		String unquoted = unquote(value);
		return unquoted == null ? null : unquoted.toLowerCase(Locale.ENGLISH);
	}

	private static String unquote(String value) {
		if (value == null || value.length() < 2) { return value; }
		char first = value.charAt(0);
		char last = value.charAt(value.length() - 1);
		if ((first == '"' && last == '"') || (first == '`' && last == '`') || (first == '[' && last == ']')) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	static boolean isDmlToken(int tokenKind) {
		return tokenKind == CCJSqlParserConstants.K_INSERT || tokenKind == CCJSqlParserConstants.K_UPDATE
				|| tokenKind == CCJSqlParserConstants.K_DELETE || tokenKind == CCJSqlParserConstants.K_MERGE
				|| tokenKind == CCJSqlParserConstants.K_UPSERT || tokenKind == CCJSqlParserConstants.K_REPLACE;
	}

	static boolean isSupportedInsert(Insert insert) {
		if (insert.isUseSet()) {
			return insert.getSetColumns() != null && insert.getSetExpressionList() != null
					&& !insert.getSetColumns().isEmpty()
					&& insert.getSetColumns().size() == insert.getSetExpressionList().size();
		}
		if (insert.getColumns() == null || insert.getColumns().isEmpty()) {
			return false;
		}
		if (insert.getSelect() == null || !(insert.getSelect().getSelectBody() instanceof SetOperationList)) {
			return false;
		}
		List<SelectBody> bodies = ((SetOperationList) insert.getSelect().getSelectBody()).getSelects();
		if (bodies.size() != 1 || !(bodies.get(0) instanceof ValuesStatement)) {
			return false;
		}
		if (!(((ValuesStatement) bodies.get(0)).getExpressions() instanceof ExpressionList)) {
			return false;
		}
		List<Expression> rows = ((ExpressionList) ((ValuesStatement) bodies.get(0)).getExpressions()).getExpressions();
		if (rows.size() != 1 || !(rows.get(0) instanceof RowConstructor)) {
			return false;
		}
		ExpressionList values = ((RowConstructor) rows.get(0)).getExprList();
		return values != null && values.getExpressions().size() == insert.getColumns().size();
	}

	private static SqlAnalysisException unsupported() {
		return new SqlAnalysisException("SQL statement is not supported for data change auditing");
	}

	static final class ParsedStatement {
		private final int statementIndex;
		private final int firstTokenKind;
		private final int parameterCount;
		private final boolean call;
		private final Statement statement;

		private ParsedStatement(int statementIndex, int firstTokenKind, int parameterCount,
								boolean call, Statement statement) {
			this.statementIndex = statementIndex;
			this.firstTokenKind = firstTokenKind;
			this.parameterCount = parameterCount;
			this.call = call;
			this.statement = statement;
		}

		int getStatementIndex() { return statementIndex; }
		int getFirstTokenKind() { return firstTokenKind; }
		int getParameterCount() { return parameterCount; }
		boolean isCall() { return call; }
		Statement getStatement() { return statement; }
	}
}
