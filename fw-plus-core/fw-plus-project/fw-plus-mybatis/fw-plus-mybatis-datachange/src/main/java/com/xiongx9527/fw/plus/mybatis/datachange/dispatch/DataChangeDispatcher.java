package com.xiongx9527.fw.plus.mybatis.datachange.dispatch;

import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DataChangeDispatcher {
	private final List<DataChangeHandler> handlers;
	private final DataChangeErrorHandler errorHandler;

	public DataChangeDispatcher(List<DataChangeHandler> handlers, DataChangeErrorHandler errorHandler) {
		this.handlers = new ArrayList<DataChangeHandler>();
		for (DataChangeHandler handler : handlers == null
				? Collections.<DataChangeHandler>emptyList() : handlers) {
			if (handler != null) {
				this.handlers.add(handler);
			}
		}
		this.errorHandler = errorHandler;
	}

	public void dispatch(DataChangeEvent event) {
		List<DataChangeHandler> orderedHandlers = new ArrayList<DataChangeHandler>(handlers);
		Collections.sort(orderedHandlers, new Comparator<DataChangeHandler>() {
			@Override
			public int compare(DataChangeHandler first, DataChangeHandler second) {
				return Integer.compare(first.getOrder(), second.getOrder());
			}
		});
		for (DataChangeHandler handler : orderedHandlers) {
			if (handler.supports(event)) {
				dispatch(handler, event);
			}
		}
	}

	private void dispatch(DataChangeHandler handler, DataChangeEvent event) {
		try (DataChangeScope ignored = DataChangeScope.suppress()) {
			handler.handle(event);
		} catch (Exception cause) {
			DataChangeException exception = new DataChangeException(DataChangeErrorCode.HANDLER_FAILED,
					"HANDLER_FAILED: " + handler.getClass().getName());
			if (event != null && event.getConsistencyLevel() == ConsistencyLevel.TRANSACTIONAL) {
				throw exception;
			}
			report(event, exception);
		}
	}

	private void report(DataChangeEvent event, DataChangeException exception) {
		if (errorHandler != null) {
			try (DataChangeScope ignored = DataChangeScope.suppress()) {
				errorHandler.handle(new DataChangeFailure(event, exception));
			} catch (Exception ignored) {
				// Best-effort dispatch cannot let error reporting stop later handlers.
			}
		}
	}
}
