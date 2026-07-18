package com.vesoft.nebula.jdbc.mysql.command;

import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlOkPacket;
import com.vesoft.nebula.jdbc.mysql.prepare.MysqlPreparedStatement;
import com.vesoft.nebula.jdbc.mysql.prepare.MysqlPreparedStatementHandler;
import com.vesoft.nebula.jdbc.mysql.query.AdvancedQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.MysqlErrorMapper;
import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewayException;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.ReadonlyQueryHandler;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MysqlCommandDispatcher {
	private final AdvancedQueryHandler queryHandler;
	private final MysqlPreparedStatementHandler preparedStatementHandler;

	public MysqlCommandDispatcher(ReadonlyQueryHandler queryHandler) {
		this(new AdvancedQueryHandlerAdapter(queryHandler));
	}

	public MysqlCommandDispatcher(AdvancedQueryHandler queryHandler) {
		this.queryHandler = queryHandler;
		this.preparedStatementHandler = new MysqlPreparedStatementHandler(queryHandler);
	}

	public MysqlCommandResult dispatch(MysqlProtocolSession session, int command, String argument) {
		try {
			if (!session.isAuthenticated()) {
				throw MysqlErrorMapper.accessDenied();
			}
			switch (command) {
				case MysqlCommand.COM_QUERY:
					MysqlQueryResult result = queryHandler.handle(session, argument);
					if (result.isOkResult()) {
						return MysqlCommandResult.ok(MysqlOkPacket.success(result.getAffectedRows(), result.getWarnings()).toPayload());
					}
					return MysqlCommandResult.resultSet(result);
				case MysqlCommand.COM_PING:
					return MysqlCommandResult.ok(MysqlOkPacket.success().toPayload());
				case MysqlCommand.COM_QUIT:
					session.close();
					return MysqlCommandResult.ok(MysqlOkPacket.success().toPayload());
				case MysqlCommand.COM_INIT_DB:
					session.setDatabase(argument);
					return MysqlCommandResult.ok(MysqlOkPacket.success().toPayload());
				case MysqlCommand.COM_STMT_PREPARE:
					MysqlPreparedStatement statement = preparedStatementHandler.prepare(session, argument);
					return MysqlCommandResult.ok(MysqlOkPacket.success(statement.getStatementId(), 0).toPayload());
				case MysqlCommand.COM_STMT_EXECUTE:
					MysqlQueryResult executeResult = executePrepared(session, argument);
					if (executeResult.isOkResult()) {
						return MysqlCommandResult.ok(MysqlOkPacket.success(executeResult.getAffectedRows(), executeResult.getWarnings()).toPayload());
					}
					return MysqlCommandResult.resultSet(executeResult);
				case MysqlCommand.COM_STMT_RESET:
					preparedStatementHandler.reset(session, parseStatementId(argument));
					return MysqlCommandResult.ok(MysqlOkPacket.success().toPayload());
				case MysqlCommand.COM_STMT_CLOSE:
					preparedStatementHandler.close(session, parseStatementId(argument));
					return MysqlCommandResult.ok(MysqlOkPacket.success().toPayload());
				default:
					throw MysqlErrorMapper.unsupported("Command is not supported");
			}
		} catch (SQLException exception) {
			MysqlGatewayException mapped = MysqlErrorMapper.execution(exception);
			return MysqlCommandResult.error(
					mapped.getErrorCode(),
					mapped.getSQLState(),
					mapped.getMessage(),
					MysqlErrorMapper.toErrPacket(mapped).toPayload()
			);
		}
	}

	private MysqlQueryResult executePrepared(MysqlProtocolSession session, String argument) throws SQLException {
		List<String> parts = splitPreparedArgument(argument);
		if (parts.isEmpty()) {
			throw MysqlErrorMapper.incorrectArguments("Prepared execute argument must include statement id");
		}
		int statementId = parseStatementId(parts.get(0));
		List<Object> parameters = new ArrayList<Object>();
		for (int i = 1; i < parts.size(); i++) {
			parameters.add(parseParameter(parts.get(i)));
		}
		return preparedStatementHandler.execute(session, statementId, parameters);
	}

	private int parseStatementId(String value) throws SQLException {
		try {
			return Integer.parseInt(value == null ? "" : value.trim());
		} catch (NumberFormatException exception) {
			throw MysqlErrorMapper.incorrectArguments("Invalid prepared statement id");
		}
	}

	private List<String> splitPreparedArgument(String argument) {
		List<String> parts = new ArrayList<String>();
		if (argument == null || argument.length() == 0) {
			return parts;
		}
		String[] split = argument.split("\\t", -1);
		for (String part : split) {
			parts.add(part);
		}
		return parts;
	}

	private Object parseParameter(String value) {
		if (value == null || "NULL".equalsIgnoreCase(value)) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException ignored) {
			return value;
		}
	}

	private static class AdvancedQueryHandlerAdapter extends AdvancedQueryHandler {
		private final ReadonlyQueryHandler readonlyQueryHandler;

		private AdvancedQueryHandlerAdapter(ReadonlyQueryHandler readonlyQueryHandler) {
			super(null, null);
			this.readonlyQueryHandler = readonlyQueryHandler;
		}

		@Override
		public MysqlQueryResult handle(MysqlProtocolSession session, String sql) throws SQLException {
			return readonlyQueryHandler.handle(session, sql);
		}
	}
}
