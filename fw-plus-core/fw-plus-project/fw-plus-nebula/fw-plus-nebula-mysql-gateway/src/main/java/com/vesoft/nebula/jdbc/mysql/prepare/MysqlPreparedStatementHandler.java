package com.vesoft.nebula.jdbc.mysql.prepare;

import com.vesoft.nebula.jdbc.mysql.query.AdvancedQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.MysqlErrorMapper;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;

public class MysqlPreparedStatementHandler {
	private final AdvancedQueryHandler queryHandler;
	private final MysqlPreparedStatementRegistry registry = new MysqlPreparedStatementRegistry();

	public MysqlPreparedStatementHandler(AdvancedQueryHandler queryHandler) {
		this.queryHandler = queryHandler;
	}

	public MysqlPreparedStatement prepare(MysqlProtocolSession session, String sql) throws SQLException {
		if (sql == null || sql.trim().length() == 0) {
			throw MysqlErrorMapper.unsupported("Prepared SQL must not be blank");
		}
		return registry.add(session.getConnectionId(), sql, countParameterMarkers(sql));
	}

	public MysqlQueryResult execute(MysqlProtocolSession session, int statementId, List<Object> parameters)
			throws SQLException {
		MysqlPreparedStatement statement = get(session, statementId);
		List<Object> safeParameters = parameters == null ? java.util.Collections.<Object>emptyList() : parameters;
		if (statement.getParameterCount() != safeParameters.size()) {
			throw MysqlErrorMapper.incorrectArguments("Prepared statement parameter count does not match");
		}
		return queryHandler.handle(session, render(statement.getSql(), safeParameters));
	}

	public MysqlPreparedStatement get(MysqlProtocolSession session, int statementId) throws SQLException {
		MysqlPreparedStatement statement = registry.get(session.getConnectionId(), statementId);
		if (statement == null) {
			throw MysqlErrorMapper.incorrectArguments("Unknown prepared statement id: " + statementId);
		}
		return statement;
	}

	public boolean contains(MysqlProtocolSession session, int statementId) {
		return registry.contains(session.getConnectionId(), statementId);
	}

	public void reset(MysqlProtocolSession session, int statementId) throws SQLException {
		get(session, statementId);
	}

	public void close(MysqlProtocolSession session, int statementId) {
		registry.remove(session.getConnectionId(), statementId);
	}

	private int countParameterMarkers(String sql) {
		return renderAwareScan(sql, null, true);
	}

	private String render(String sql, List<Object> parameters) throws SQLException {
		StringBuilder rendered = new StringBuilder(sql.length());
		renderAwareScan(sql, new RenderContext(rendered, parameters), false);
		return rendered.toString();
	}

	private int renderAwareScan(String sql, RenderContext renderContext, boolean countOnly) {
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean backtickQuoted = false;
		boolean lineComment = false;
		boolean blockComment = false;
		int parameterIndex = 0;
		for (int i = 0; i < sql.length(); i++) {
			char current = sql.charAt(i);
			char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

			if (lineComment) {
				append(renderContext, current);
				if (current == '\n' || current == '\r') {
					lineComment = false;
				}
				continue;
			}
			if (blockComment) {
				append(renderContext, current);
				if (current == '*' && next == '/') {
					append(renderContext, next);
					blockComment = false;
					i++;
				}
				continue;
			}
			if (singleQuoted) {
				append(renderContext, current);
				if (current == '\\' && next != '\0') {
					append(renderContext, next);
					i++;
				} else if (current == '\'' && next == '\'') {
					append(renderContext, next);
					i++;
				} else if (current == '\'') {
					singleQuoted = false;
				}
				continue;
			}
			if (doubleQuoted) {
				append(renderContext, current);
				if (current == '\\' && next != '\0') {
					append(renderContext, next);
					i++;
				} else if (current == '"' && next == '"') {
					append(renderContext, next);
					i++;
				} else if (current == '"') {
					doubleQuoted = false;
				}
				continue;
			}
			if (backtickQuoted) {
				append(renderContext, current);
				if (current == '`') {
					backtickQuoted = false;
				}
				continue;
			}

			if (current == '-' && next == '-') {
				append(renderContext, current);
				append(renderContext, next);
				lineComment = true;
				i++;
			} else if (current == '/' && next == '*') {
				append(renderContext, current);
				append(renderContext, next);
				blockComment = true;
				i++;
			} else if (current == '\'') {
				append(renderContext, current);
				singleQuoted = true;
			} else if (current == '"') {
				append(renderContext, current);
				doubleQuoted = true;
			} else if (current == '`') {
				append(renderContext, current);
				backtickQuoted = true;
			} else if (current == '?') {
				if (!countOnly) {
					renderContext.builder.append(toLiteral(renderContext.parameters.get(parameterIndex)));
				}
				parameterIndex++;
			} else {
				append(renderContext, current);
			}
		}
		return parameterIndex;
	}

	private void append(RenderContext renderContext, char value) {
		if (renderContext != null) {
			renderContext.builder.append(value);
		}
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
		return "\"" + String.valueOf(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	private static class RenderContext {
		private final StringBuilder builder;
		private final List<Object> parameters;

		private RenderContext(StringBuilder builder, List<Object> parameters) {
			this.builder = builder;
			this.parameters = parameters;
		}
	}
}
