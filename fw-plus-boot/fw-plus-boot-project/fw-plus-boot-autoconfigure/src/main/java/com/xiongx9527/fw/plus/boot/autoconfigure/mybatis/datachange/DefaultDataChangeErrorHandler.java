package com.xiongx9527.fw.plus.boot.autoconfigure.mybatis.datachange;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DefaultDataChangeErrorHandler implements DataChangeErrorHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataChangeErrorHandler.class);

	@Override
	public void handle(DataChangeFailure failure) {
		String eventId = UUID.randomUUID().toString();
		DataChangeEvent event = failure == null ? null : failure.getEvent();
		DataChangeException exception = failure == null ? null : failure.getException();
		List<StatementChange> statements = event == null
				? Collections.<StatementChange>emptyList() : event.getStatements();
		if (statements.isEmpty()) {
			log(eventId, event, null, null, failure, exception);
			return;
		}
		for (StatementChange statement : statements) {
			List<TableChange> tables = statement == null
					? Collections.<TableChange>emptyList() : statement.getTables();
			if (tables.isEmpty()) {
				log(eventId, event, statement, null, failure, exception);
				continue;
			}
			for (TableChange table : tables) {
				log(eventId, event, statement, table, failure, exception);
			}
		}
	}

	private void log(String eventId, DataChangeEvent event, StatementChange statement, TableChange table,
			DataChangeFailure failure, DataChangeException exception) {
		LOGGER.error(formatMessage(eventId, event, statement, table, failure, exception));

	}

	String formatMessage(String eventId, DataChangeEvent event, StatementChange statement, TableChange table,
			DataChangeFailure failure, DataChangeException exception) {
		return String.format("Data change handling failed: eventId=%s, statementId=%s, table=%s, consistency=%s, "
				+ "errorCode=%s, exceptionType=%s, exceptionMessage=%s",
				eventId,
				statement == null ? null : statement.getStatementId(),
				table == null ? null : table.getTable(),
				event == null ? null : event.getConsistencyLevel(),
				failure == null ? null : failure.getCode(),
				exception == null ? null : exception.getClass().getName(),
				exception == null ? null : sanitize(exception.getMessage()));
	}

	private String sanitize(String message) {
		return message == null ? null : message.replace('\r', ' ').replace('\n', ' ');
	}
}
