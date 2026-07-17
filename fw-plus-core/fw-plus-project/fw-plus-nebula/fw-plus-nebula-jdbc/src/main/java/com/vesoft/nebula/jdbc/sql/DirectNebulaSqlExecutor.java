package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.statement.NebulaStatementClassifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class DirectNebulaSqlExecutor implements NebulaSqlExecutor {
	@Override
	public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkQuery(sql);
		return new NebulaResultSet(executeNative(connection, renderSql(sql, bindings)), null);
	}

	@Override
	public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkUpdate(sql);
		executeNative(connection, renderSql(sql, bindings));
		return 0;
	}

	private com.vesoft.nebula.client.graph.data.ResultSet executeNative(NebulaConnection connection, String sql)
			throws SQLException {
		com.vesoft.nebula.client.graph.data.ResultSet resultSet = connection.execute(sql);
		if (!resultSet.isSucceeded()) {
			throw new SQLException(String.format(
					"nGql \"%s\" executed failed, error code: %d, error message: %s",
					sql,
					resultSet.getErrorCode(),
					resultSet.getErrorMessage()
			));
		}
		return resultSet;
	}

	private String renderSql(String sql, NebulaParameterBindings bindings) throws SQLException {
		if (bindings == null || bindings.size() == 0) {
			return sql;
		}
		bindings.validateComplete();
		StringBuilder rendered = new StringBuilder(sql.length());
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean backtickQuoted = false;
		boolean lineComment = false;
		boolean blockComment = false;
		int parameterIndex = 1;
		for (int i = 0; i < sql.length(); i++) {
			char current = sql.charAt(i);
			char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

			if (lineComment) {
				rendered.append(current);
				if (current == '\n' || current == '\r') {
					lineComment = false;
				}
				continue;
			}
			if (blockComment) {
				rendered.append(current);
				if (current == '*' && next == '/') {
					rendered.append(next);
					blockComment = false;
					i++;
				}
				continue;
			}
			if (singleQuoted) {
				rendered.append(current);
				if (current == '\\' && next != '\0') {
					rendered.append(next);
					i++;
				} else if (current == '\'' && next == '\'') {
					rendered.append(next);
					i++;
				} else if (current == '\'') {
					singleQuoted = false;
				}
				continue;
			}
			if (doubleQuoted) {
				rendered.append(current);
				if (current == '\\' && next != '\0') {
					rendered.append(next);
					i++;
				} else if (current == '"' && next == '"') {
					rendered.append(next);
					i++;
				} else if (current == '"') {
					doubleQuoted = false;
				}
				continue;
			}
			if (backtickQuoted) {
				rendered.append(current);
				if (current == '`') {
					backtickQuoted = false;
				}
				continue;
			}

			if (current == '-' && next == '-') {
				rendered.append(current).append(next);
				lineComment = true;
				i++;
			} else if (current == '/' && next == '*') {
				rendered.append(current).append(next);
				blockComment = true;
				i++;
			} else if (current == '\'') {
				rendered.append(current);
				singleQuoted = true;
			} else if (current == '"') {
				rendered.append(current);
				doubleQuoted = true;
			} else if (current == '`') {
				rendered.append(current);
				backtickQuoted = true;
			} else if (current == '?') {
				if (parameterIndex > bindings.size()) {
					throw new SQLException("More parameter markers than bound parameters.");
				}
				rendered.append(toLiteral(bindings.get(parameterIndex).getValue()));
				parameterIndex++;
			} else {
				rendered.append(current);
			}
		}
		if (parameterIndex <= bindings.size()) {
			throw new SQLException("Fewer parameter markers than bound parameters.");
		}
		return rendered.toString();
	}

	private String toLiteral(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof Number || value instanceof Boolean) {
			return String.valueOf(value);
		}
		if (value instanceof java.sql.Date) {
			return "date(\"" + value + "\")";
		}
		if (value instanceof Time) {
			return "time(\"" + value + "\")";
		}
		if (value instanceof Timestamp) {
			return "datetime(\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(value) + "\")";
		}
		if (value instanceof java.util.Date) {
			return "datetime(\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(value) + "\")";
		}
		return "\"" + escapeString(String.valueOf(value)) + "\"";
	}

	private String escapeString(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
