package com.vesoft.nebula.jdbc.mysql.command;

import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.protocol.MysqlOkPacket;
import com.vesoft.nebula.jdbc.mysql.query.AdvancedQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.MysqlErrorMapper;
import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewayException;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.ReadonlyQueryHandler;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;

import java.sql.SQLException;

public class MysqlCommandDispatcher {
	private final AdvancedQueryHandler queryHandler;

	public MysqlCommandDispatcher(ReadonlyQueryHandler queryHandler) {
		this(new AdvancedQueryHandlerAdapter(queryHandler));
	}

	public MysqlCommandDispatcher(AdvancedQueryHandler queryHandler) {
		this.queryHandler = queryHandler;
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
