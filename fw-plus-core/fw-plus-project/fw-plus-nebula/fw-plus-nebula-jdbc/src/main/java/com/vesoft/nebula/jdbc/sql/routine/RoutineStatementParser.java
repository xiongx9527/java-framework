package com.vesoft.nebula.jdbc.sql.routine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RoutineStatementParser {

	private static final Pattern CREATE_PROCEDURE = Pattern.compile(
			"(?is)^CREATE\\s+PROCEDURE\\s+((?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)(?:\\s*\\.\\s*(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*))?)\\s*\\((.*)\\)\\s+BEGIN\\s+(.*)\\s+END\\s*$");
	private static final Pattern DROP_PROCEDURE = Pattern.compile(
			"(?is)^DROP\\s+PROCEDURE\\s+(IF\\s+EXISTS\\s+)?((?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)(?:\\s*\\.\\s*(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*))?)\\s*$");
	private static final Pattern CALL_PROCEDURE = Pattern.compile(
			"(?is)^CALL\\s+((?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*)(?:\\s*\\.\\s*(?:`[^`]+`|[A-Za-z_][A-Za-z0-9_$]*))?)\\s*\\((.*)\\)\\s*$");

	private RoutineStatementParser() {
	}

	public static boolean isRoutineStatement(String sql) {
		if (sql == null) {
			return false;
		}
		String upper = stripTrailingSemicolon(sql).trim().toUpperCase(Locale.ROOT);
		return upper.startsWith("CREATE PROCEDURE")
				|| upper.startsWith("DROP PROCEDURE")
				|| upper.startsWith("CALL ")
				|| upper.startsWith("CREATE FUNCTION")
				|| upper.startsWith("ALTER PROCEDURE")
				|| upper.startsWith("SHOW PROCEDURE");
	}

	public static RoutineStatement parse(String sql) throws SQLException {
		if (sql == null || sql.trim().isEmpty()) {
			throw RoutineException.syntax("Routine SQL is empty");
		}
		String trimmed = stripTrailingSemicolon(sql).trim();
		rejectUnsupported(trimmed);

		Matcher create = CREATE_PROCEDURE.matcher(trimmed);
		if (create.matches()) {
			String name = RoutineNames.displayName(create.group(1));
			return new RoutineStatement(RoutineStatement.Kind.CREATE_PROCEDURE, name, false,
					parseParameters(create.group(2)), splitBody(create.group(3)),
					Collections.emptyList(), sql);
		}

		Matcher drop = DROP_PROCEDURE.matcher(trimmed);
		if (drop.matches()) {
			String name = RoutineNames.displayName(drop.group(2));
			return new RoutineStatement(RoutineStatement.Kind.DROP_PROCEDURE, name,
					drop.group(1) != null, Collections.<RoutineParameter>emptyList(),
					Collections.<String>emptyList(), Collections.emptyList(), sql);
		}

		Matcher call = CALL_PROCEDURE.matcher(trimmed);
		if (call.matches()) {
			String name = RoutineNames.displayName(call.group(1));
			return new RoutineStatement(RoutineStatement.Kind.CALL_PROCEDURE, name, false,
					Collections.<RoutineParameter>emptyList(), Collections.<String>emptyList(),
					parseArguments(call.group(2)), sql);
		}

		throw RoutineException.syntax("Unsupported routine syntax");
	}

	private static void rejectUnsupported(String sql) throws SQLException {
		String upper = sql.toUpperCase(Locale.ROOT);
		if (upper.startsWith("CREATE FUNCTION")) {
			throw RoutineException.unsupported("CREATE FUNCTION");
		}
		if (upper.startsWith("ALTER PROCEDURE") || upper.startsWith("SHOW PROCEDURE")) {
			throw RoutineException.unsupported(firstWords(upper, 2));
		}
		String padded = unsupportedKeywordSearchText(sql);
		String[] unsupported = new String[] {
				" OUT ", " INOUT ", " CURSOR ", " HANDLER ", " PREPARE ", " EXECUTE ",
				" DEALLOCATE ", " CREATE TEMPORARY TABLE ", " START TRANSACTION ",
				" COMMIT ", " ROLLBACK ", " WHILE ", " REPEAT ", " LOOP ", " RETURN "
		};
		for (int i = 0; i < unsupported.length; i++) {
			if (padded.contains(unsupported[i])) {
				throw RoutineException.unsupported(unsupported[i].trim());
			}
		}
	}

	private static String unsupportedKeywordSearchText(String sql) throws SQLException {
		StringBuilder builder = new StringBuilder(sql.length() + 2);
		builder.append(' ');
		boolean inString = false;
		for (int i = 0; i < sql.length(); i++) {
			char c = sql.charAt(i);
			if (c == '\'') {
				builder.append(' ');
				if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
					builder.append(' ');
					i++;
				}
				else {
					inString = !inString;
				}
				continue;
			}
			if (inString || c == '\n' || c == '\r' || c == ';'
					|| c == '(' || c == ')' || c == ',') {
				builder.append(' ');
			}
			else {
				builder.append(Character.toUpperCase(c));
			}
		}
		if (inString) {
			throw RoutineException.syntax("Unbalanced routine expression");
		}
		builder.append(' ');
		return builder.toString();
	}

	private static String firstWords(String upper, int count) {
		String[] parts = upper.trim().split("\\s+");
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < parts.length && i < count; i++) {
			if (i > 0) {
				builder.append(' ');
			}
			builder.append(parts[i]);
		}
		return builder.toString();
	}

	private static List<RoutineParameter> parseParameters(String rawParameters) throws SQLException {
		List<String> parts = splitTopLevel(rawParameters, ',');
		List<RoutineParameter> parameters = new ArrayList<RoutineParameter>();
		for (int i = 0; i < parts.size(); i++) {
			String part = parts.get(i).trim();
			if (part.isEmpty()) {
				continue;
			}
			String[] tokens = part.split("\\s+", 3);
			String mode = "IN";
			String name;
			String type;
			if ("OUT".equalsIgnoreCase(tokens[0]) || "INOUT".equalsIgnoreCase(tokens[0])) {
				throw RoutineException.unsupported(tokens[0].toUpperCase(Locale.ROOT));
			}
			if ("IN".equalsIgnoreCase(tokens[0])) {
				if (tokens.length < 3) {
					throw RoutineException.syntax("Invalid routine parameter: " + part);
				}
				name = RoutineNames.displayName(tokens[1]);
				type = tokens[2].trim();
			}
			else {
				if (tokens.length < 2) {
					throw RoutineException.syntax("Invalid routine parameter: " + part);
				}
				name = RoutineNames.displayName(tokens[0]);
				type = part.substring(tokens[0].length()).trim();
			}
			parameters.add(new RoutineParameter(mode, name, type));
		}
		return parameters;
	}

	private static List<String> splitBody(String rawBody) throws SQLException {
		List<String> statements = splitTopLevel(rawBody, ';');
		List<String> body = new ArrayList<String>();
		for (int i = 0; i < statements.size(); i++) {
			String statement = statements.get(i).trim();
			if (!statement.isEmpty()) {
				if (startsWithIgnoreCase(statement, "IF ")) {
					StringBuilder ifBlock = new StringBuilder(statement);
					while (!endsWithIgnoreCase(statement, "END IF")) {
						i++;
						if (i >= statements.size()) {
							throw RoutineException.syntax("Unterminated IF statement");
						}
						statement = statements.get(i).trim();
						if (!statement.isEmpty()) {
							ifBlock.append("; ").append(statement);
						}
					}
					body.add(ifBlock.toString());
				}
				else {
					body.add(statement);
				}
			}
		}
		if (body.isEmpty()) {
			throw RoutineException.syntax("Routine body is empty");
		}
		return body;
	}

	private static boolean startsWithIgnoreCase(String value, String prefix) {
		return value != null && value.toUpperCase(Locale.ROOT)
				.startsWith(prefix.toUpperCase(Locale.ROOT));
	}

	private static boolean endsWithIgnoreCase(String value, String suffix) {
		return value != null && value.toUpperCase(Locale.ROOT)
				.endsWith(suffix.toUpperCase(Locale.ROOT));
	}

	private static List<Object> parseArguments(String rawArguments) throws SQLException {
		List<String> parts = splitTopLevel(rawArguments, ',');
		List<Object> arguments = new ArrayList<Object>();
		for (int i = 0; i < parts.size(); i++) {
			String part = parts.get(i).trim();
			if (part.isEmpty()) {
				continue;
			}
			if ("NULL".equalsIgnoreCase(part)) {
				arguments.add(null);
			}
			else if (isQuoted(part)) {
				arguments.add(part.substring(1, part.length() - 1).replace("''", "'"));
			}
			else {
				try {
					arguments.add(Integer.valueOf(part));
				}
				catch (NumberFormatException exception) {
					arguments.add(part);
				}
			}
		}
		return arguments;
	}

	private static boolean isQuoted(String value) {
		return value.length() >= 2 && value.startsWith("'") && value.endsWith("'");
	}

	private static List<String> splitTopLevel(String value, char separator) throws SQLException {
		List<String> parts = new ArrayList<String>();
		if (value == null || value.trim().isEmpty()) {
			return parts;
		}
		int depth = 0;
		boolean inString = false;
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c == '\'') {
				current.append(c);
				if (inString && i + 1 < value.length() && value.charAt(i + 1) == '\'') {
					current.append(value.charAt(i + 1));
					i++;
				}
				else {
					inString = !inString;
				}
				continue;
			}
			if (!inString) {
				if (c == '(') {
					depth++;
				}
				else if (c == ')') {
					depth--;
					if (depth < 0) {
						throw RoutineException.syntax("Unbalanced routine expression");
					}
				}
				else if (c == separator && depth == 0) {
					parts.add(current.toString());
					current.setLength(0);
					continue;
				}
			}
			current.append(c);
		}
		if (inString || depth != 0) {
			throw RoutineException.syntax("Unbalanced routine expression");
		}
		parts.add(current.toString());
		return parts;
	}

	private static String stripTrailingSemicolon(String sql) {
		String value = sql;
		while (value != null && value.trim().endsWith(";")) {
			int end = value.lastIndexOf(';');
			value = value.substring(0, end);
		}
		return value;
	}
}
