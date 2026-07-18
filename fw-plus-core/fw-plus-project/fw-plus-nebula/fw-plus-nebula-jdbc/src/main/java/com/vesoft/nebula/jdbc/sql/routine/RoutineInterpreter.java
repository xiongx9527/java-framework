package com.vesoft.nebula.jdbc.sql.routine;

import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.statement.NebulaStatementClassifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoutineInterpreter {

	public RoutineCallResult execute(RoutineDefinition definition, RoutineStatement call,
			RoutineExecutionContext context) throws SQLException {
		if (definition.getParameters().size() != call.getArguments().size()) {
			throw RoutineException.invalidArguments(definition.getName());
		}
		if (countBudgetedStatements(definition.getBodyStatements()) > context.getMaxStatements()) {
			throw RoutineException.executionLimit("Routine execution statement limit exceeded");
		}
		Map<String, Object> variables = bindArguments(definition, call);

		context.checkTimeout();
		ResultSet lastResultSet = null;
		int updateCount = 0;
		for (int i = 0; i < definition.getBodyStatements().size(); i++) {
			context.checkTimeout();
			StatementExecution execution = executeBodyStatement(definition.getBodyStatements().get(i),
					context, variables);
			if (execution.resultSet != null) {
				lastResultSet = execution.resultSet;
			}
			updateCount += execution.updateCount;
		}
		if (lastResultSet != null) {
			return RoutineCallResult.resultSet(lastResultSet);
		}
		return RoutineCallResult.updateCount(updateCount);
	}

	private Map<String, Object> bindArguments(RoutineDefinition definition, RoutineStatement call) {
		Map<String, Object> variables = new LinkedHashMap<String, Object>();
		for (int i = 0; i < definition.getParameters().size(); i++) {
			variables.put(definition.getParameters().get(i).getNormalizedName(),
					call.getArguments().get(i));
		}
		return variables;
	}

	private StatementExecution executeBodyStatement(String statement, RoutineExecutionContext context,
			Map<String, Object> variables) throws SQLException {
		String trimmed = statement.trim();
		if (startsWithIgnoreCase(trimmed, "DECLARE ")) {
			declareVariable(trimmed, variables);
			return StatementExecution.empty();
		}
		if (startsWithIgnoreCase(trimmed, "SET ")) {
			setVariable(trimmed, variables);
			return StatementExecution.empty();
		}
		if (startsWithIgnoreCase(trimmed, "IF ")) {
			return executeIf(trimmed, context, variables);
		}
		return executeSql(trimmed, context, variables);
	}

	private void declareVariable(String statement, Map<String, Object> variables) throws SQLException {
		String body = statement.substring("DECLARE".length()).trim();
		int defaultIndex = findKeywordOutsideQuotes(body, "DEFAULT", 0);
		String declaration = defaultIndex >= 0 ? body.substring(0, defaultIndex).trim() : body;
		String[] tokens = declaration.split("\\s+");
		if (tokens.length == 0 || tokens[0].trim().isEmpty()) {
			throw RoutineException.syntax("Invalid DECLARE statement");
		}
		Object value = null;
		if (defaultIndex >= 0) {
			value = evaluateExpression(body.substring(defaultIndex + "DEFAULT".length()).trim(), variables);
		}
		variables.put(RoutineNames.normalize(tokens[0]), value);
	}

	private void setVariable(String statement, Map<String, Object> variables) throws SQLException {
		String body = statement.substring("SET".length()).trim();
		int equals = indexOfOutsideQuotes(body, "=");
		if (equals < 0) {
			throw RoutineException.syntax("Invalid SET statement");
		}
		String name = body.substring(0, equals).trim();
		variables.put(RoutineNames.normalize(name),
				evaluateExpression(body.substring(equals + 1).trim(), variables));
	}

	private StatementExecution executeIf(String statement, RoutineExecutionContext context,
			Map<String, Object> variables) throws SQLException {
		List<IfBranch> branches = parseIfBranches(statement);
		for (int i = 0; i < branches.size(); i++) {
			IfBranch branch = branches.get(i);
			if (branch.condition == null || evaluateCondition(branch.condition, variables)) {
				return executeStatements(splitBranchStatements(branch.body), context, variables);
			}
		}
		return StatementExecution.empty();
	}

	private StatementExecution executeStatements(List<String> statements, RoutineExecutionContext context,
			Map<String, Object> variables) throws SQLException {
		StatementExecution combined = StatementExecution.empty();
		for (int i = 0; i < statements.size(); i++) {
			context.checkTimeout();
			StatementExecution execution = executeBodyStatement(statements.get(i), context, variables);
			if (execution.resultSet != null) {
				combined.resultSet = execution.resultSet;
			}
			combined.updateCount += execution.updateCount;
		}
		return combined;
	}

	private StatementExecution executeSql(String sql, RoutineExecutionContext context,
			Map<String, Object> variables) throws SQLException {
		if (startsWithCall(sql)) {
			throw RoutineException.unsupported("routine recursion or nested CALL");
		}
		PreparedRoutineSql prepared = prepareSql(sql, variables);
		if (isQuery(prepared.sql)) {
			ResultSet resultSet = context.getExecutor().executeQuery(context.getConnection(),
					prepared.sql, prepared.bindings);
			context.checkTimeout();
			return StatementExecution.resultSet(resultSet);
		}
		int updateCount = context.getExecutor().executeUpdate(context.getConnection(),
				prepared.sql, prepared.bindings);
		context.checkTimeout();
		return StatementExecution.updateCount(updateCount);
	}

	private int countBudgetedStatements(List<String> statements) throws SQLException {
		int count = 0;
		for (int i = 0; i < statements.size(); i++) {
			String statement = statements.get(i).trim();
			if (startsWithIgnoreCase(statement, "IF ")) {
				count += maxBranchStatementCount(parseIfBranches(statement));
			}
			else {
				count++;
			}
		}
		return count;
	}

	private int maxBranchStatementCount(List<IfBranch> branches) throws SQLException {
		int max = 0;
		for (int i = 0; i < branches.size(); i++) {
			max = Math.max(max, countBudgetedStatements(splitBranchStatements(branches.get(i).body)));
		}
		return max;
	}

	private PreparedRoutineSql prepareSql(String sql, Map<String, Object> variables) throws SQLException {
		StringBuilder rendered = new StringBuilder();
		List<Object> values = new ArrayList<Object>();
		boolean inString = false;
		for (int i = 0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '\'') {
				rendered.append(c);
				if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
					rendered.append(sql.charAt(i + 1));
					i++;
				}
				else {
					inString = !inString;
				}
				continue;
			}
			if (!inString && isIdentifierStart(c)) {
				int start = i;
				i++;
				while (i < sql.length() && isIdentifierPart(sql.charAt(i))) {
					i++;
				}
				String token = sql.substring(start, i);
				String normalized = RoutineNames.normalize(token);
				if (variables.containsKey(normalized) && isValuePosition(sql, start, i)) {
					rendered.append('?');
					values.add(variables.get(normalized));
				}
				else {
					rendered.append(token);
				}
				i--;
				continue;
			}
			rendered.append(c);
		}
		NebulaParameterBindings bindings = new NebulaParameterBindings(values.size());
		for (int i = 0; i < values.size(); i++) {
			bindings.bind(i + 1, values.get(i), jdbcType(values.get(i)));
		}
		return new PreparedRoutineSql(rendered.toString(), bindings);
	}

	private boolean isValuePosition(String sql, int start, int end) {
		char previous = previousSignificant(sql, start);
		char next = nextSignificant(sql, end);
		if (next == '=') {
			return false;
		}
		return previous == '=' || previous == '<' || previous == '>' || previous == '!'
				|| previous == '(' || previous == ',';
	}

	private List<IfBranch> parseIfBranches(String statement) throws SQLException {
		String body = statement.trim();
		if (!endsWithIgnoreCase(body, "END IF")) {
			throw RoutineException.syntax("Unterminated IF statement");
		}
		body = body.substring(2, body.length() - "END IF".length()).trim();
		List<IfBranch> branches = new ArrayList<IfBranch>();
		int thenIndex = findKeywordOutsideQuotes(body, "THEN", 0);
		if (thenIndex < 0) {
			throw RoutineException.syntax("Invalid IF statement");
		}
		String condition = body.substring(0, thenIndex).trim();
		int cursor = thenIndex + "THEN".length();
		while (true) {
			int elseIfIndex = findKeywordOutsideQuotes(body, "ELSEIF", cursor);
			int elseIndex = findKeywordOutsideQuotes(body, "ELSE", cursor);
			int nextIndex = earliestNonNegative(elseIfIndex, elseIndex);
			if (nextIndex < 0) {
				branches.add(new IfBranch(condition, body.substring(cursor).trim()));
				break;
			}
			branches.add(new IfBranch(condition, body.substring(cursor, nextIndex).trim()));
			if (nextIndex == elseIfIndex) {
				int nextThen = findKeywordOutsideQuotes(body, "THEN", nextIndex + "ELSEIF".length());
				if (nextThen < 0) {
					throw RoutineException.syntax("Invalid ELSEIF statement");
				}
				condition = body.substring(nextIndex + "ELSEIF".length(), nextThen).trim();
				cursor = nextThen + "THEN".length();
			}
			else {
				branches.add(new IfBranch(null, body.substring(nextIndex + "ELSE".length()).trim()));
				break;
			}
		}
		return branches;
	}

	private List<String> splitBranchStatements(String body) {
		List<String> statements = new ArrayList<String>();
		String[] parts = body.split(";");
		for (int i = 0; i < parts.length; i++) {
			String statement = parts[i].trim();
			if (!statement.isEmpty()) {
				statements.add(statement);
			}
		}
		return statements;
	}

	private boolean evaluateCondition(String condition, Map<String, Object> variables) throws SQLException {
		int orIndex = findKeywordOutsideQuotes(condition, "OR", 0);
		if (orIndex >= 0) {
			return evaluateCondition(condition.substring(0, orIndex), variables)
					|| evaluateCondition(condition.substring(orIndex + 2), variables);
		}
		int andIndex = findKeywordOutsideQuotes(condition, "AND", 0);
		if (andIndex >= 0) {
			return evaluateCondition(condition.substring(0, andIndex), variables)
					&& evaluateCondition(condition.substring(andIndex + 3), variables);
		}
		String trimmed = stripParentheses(condition.trim());
		int isNotNull = findKeywordOutsideQuotes(trimmed, "IS NOT NULL", 0);
		if (isNotNull >= 0) {
			return evaluateExpression(trimmed.substring(0, isNotNull).trim(), variables) != null;
		}
		int isNull = findKeywordOutsideQuotes(trimmed, "IS NULL", 0);
		if (isNull >= 0) {
			return evaluateExpression(trimmed.substring(0, isNull).trim(), variables) == null;
		}
		String[] operators = new String[] {">=", "<=", "<>", "!=", "=", ">", "<"};
		for (int i = 0; i < operators.length; i++) {
			int index = indexOfOutsideQuotes(trimmed, operators[i]);
			if (index >= 0) {
				Object left = evaluateExpression(trimmed.substring(0, index).trim(), variables);
				Object right = evaluateExpression(trimmed.substring(index + operators[i].length()).trim(), variables);
				return compare(left, right, operators[i]);
			}
		}
		Object value = evaluateExpression(trimmed, variables);
		return Boolean.TRUE.equals(value);
	}

	private Object evaluateExpression(String expression, Map<String, Object> variables) {
		String value = stripParentheses(expression.trim());
		if ("NULL".equalsIgnoreCase(value)) {
			return null;
		}
		if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
			return value.substring(1, value.length() - 1).replace("''", "'");
		}
		String normalized = RoutineNames.normalize(value);
		if (variables.containsKey(normalized)) {
			return variables.get(normalized);
		}
		try {
			return Integer.valueOf(value);
		}
		catch (NumberFormatException exception) {
			return value;
		}
	}

	private boolean compare(Object left, Object right, String operator) {
		if ("=".equals(operator)) {
			return left == null ? right == null : left.equals(right);
		}
		if ("<>".equals(operator) || "!=".equals(operator)) {
			return !(left == null ? right == null : left.equals(right));
		}
		if (left == null || right == null) {
			return false;
		}
		int compared = compareValues(left, right);
		if (">".equals(operator)) {
			return compared > 0;
		}
		if (">=".equals(operator)) {
			return compared >= 0;
		}
		if ("<".equals(operator)) {
			return compared < 0;
		}
		if ("<=".equals(operator)) {
			return compared <= 0;
		}
		return false;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private int compareValues(Object left, Object right) {
		if (left instanceof Number && right instanceof Number) {
			return Long.valueOf(((Number) left).longValue())
					.compareTo(Long.valueOf(((Number) right).longValue()));
		}
		if (left instanceof Comparable && left.getClass().isInstance(right)) {
			return ((Comparable) left).compareTo(right);
		}
		return String.valueOf(left).compareTo(String.valueOf(right));
	}

	private boolean isQuery(String sql) throws SQLException {
		try {
			NebulaStatementClassifier.checkQuery(sql);
			return true;
		}
		catch (SQLException queryException) {
			NebulaStatementClassifier.checkUpdate(sql);
			return false;
		}
	}

	private boolean startsWithCall(String sql) {
		return sql != null && sql.trim().toUpperCase(Locale.ROOT).startsWith("CALL ");
	}

	private boolean startsWithIgnoreCase(String value, String prefix) {
		return value != null && value.toUpperCase(Locale.ROOT)
				.startsWith(prefix.toUpperCase(Locale.ROOT));
	}

	private boolean endsWithIgnoreCase(String value, String suffix) {
		return value != null && value.toUpperCase(Locale.ROOT)
				.endsWith(suffix.toUpperCase(Locale.ROOT));
	}

	private int findKeywordOutsideQuotes(String value, String keyword, int start) {
		String upperKeyword = keyword.toUpperCase(Locale.ROOT);
		boolean inString = false;
		for (int i = start; i <= value.length() - keyword.length(); i++) {
			char c = value.charAt(i);
			if (c == '\'') {
				if (inString && i + 1 < value.length() && value.charAt(i + 1) == '\'') {
					i++;
				}
				else {
					inString = !inString;
				}
				continue;
			}
			if (!inString && value.regionMatches(true, i, keyword, 0, keyword.length())
					&& hasKeywordBoundary(value, i, keyword.length())) {
				return i;
			}
		}
		return -1;
	}

	private boolean hasKeywordBoundary(String value, int start, int length) {
		int before = start - 1;
		int after = start + length;
		return (before < 0 || !isIdentifierPart(value.charAt(before)))
				&& (after >= value.length() || !isIdentifierPart(value.charAt(after)));
	}

	private int indexOfOutsideQuotes(String value, String needle) {
		boolean inString = false;
		for (int i = 0; i <= value.length() - needle.length(); i++) {
			char c = value.charAt(i);
			if (c == '\'') {
				if (inString && i + 1 < value.length() && value.charAt(i + 1) == '\'') {
					i++;
				}
				else {
					inString = !inString;
				}
				continue;
			}
			if (!inString && value.regionMatches(i, needle, 0, needle.length())) {
				return i;
			}
		}
		return -1;
	}

	private int earliestNonNegative(int first, int second) {
		if (first < 0) {
			return second;
		}
		if (second < 0) {
			return first;
		}
		return Math.min(first, second);
	}

	private String stripParentheses(String value) {
		String result = value;
		while (result.startsWith("(") && result.endsWith(")")) {
			result = result.substring(1, result.length() - 1).trim();
		}
		return result;
	}

	private boolean isIdentifierStart(char c) {
		return Character.isLetter(c) || c == '_' || c == '$';
	}

	private boolean isIdentifierPart(char c) {
		return Character.isLetterOrDigit(c) || c == '_' || c == '$';
	}

	private char previousSignificant(String value, int start) {
		for (int i = start - 1; i >= 0; i--) {
			if (!Character.isWhitespace(value.charAt(i))) {
				return value.charAt(i);
			}
		}
		return '\0';
	}

	private char nextSignificant(String value, int end) {
		for (int i = end; i < value.length(); i++) {
			if (!Character.isWhitespace(value.charAt(i))) {
				return value.charAt(i);
			}
		}
		return '\0';
	}

	private int jdbcType(Object value) {
		if (value == null) {
			return Types.NULL;
		}
		if (value instanceof Integer) {
			return Types.INTEGER;
		}
		if (value instanceof Long) {
			return Types.BIGINT;
		}
		if (value instanceof Boolean) {
			return Types.BOOLEAN;
		}
		return Types.VARCHAR;
	}

	private static class PreparedRoutineSql {
		private final String sql;
		private final NebulaParameterBindings bindings;

		private PreparedRoutineSql(String sql, NebulaParameterBindings bindings) {
			this.sql = sql;
			this.bindings = bindings;
		}
	}

	private static class StatementExecution {
		private ResultSet resultSet;
		private int updateCount;

		private static StatementExecution empty() {
			return new StatementExecution();
		}

		private static StatementExecution resultSet(ResultSet resultSet) {
			StatementExecution execution = new StatementExecution();
			execution.resultSet = resultSet;
			return execution;
		}

		private static StatementExecution updateCount(int updateCount) {
			StatementExecution execution = new StatementExecution();
			execution.updateCount = updateCount;
			return execution;
		}
	}

	private static class IfBranch {
		private final String condition;
		private final String body;

		private IfBranch(String condition, String body) {
			this.condition = condition;
			this.body = body;
		}
	}
}
