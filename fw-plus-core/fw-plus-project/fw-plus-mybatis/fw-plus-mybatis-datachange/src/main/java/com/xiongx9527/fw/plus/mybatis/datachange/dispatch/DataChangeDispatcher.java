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
		List<OrderedHandler> orderedHandlers = new ArrayList<OrderedHandler>();
		for (DataChangeHandler handler : handlers) {
			Integer order = order(handler, event);
			if (order != null) { orderedHandlers.add(new OrderedHandler(handler, order.intValue())); }
		}
		Collections.sort(orderedHandlers, new Comparator<OrderedHandler>() {
			@Override
			public int compare(OrderedHandler first, OrderedHandler second) {
				return Integer.compare(first.order, second.order);
			}
		});
		for (OrderedHandler ordered : orderedHandlers) {
			dispatch(ordered.handler, event);
		}
	}

	private Integer order(DataChangeHandler handler, DataChangeEvent event) {
		try (DataChangeScope ignored = DataChangeScope.suppress()) {
			return Integer.valueOf(handler.getOrder());
		} catch (Exception cause) {
			failure(handler, event);
			return null;
		}
	}

	private void dispatch(DataChangeHandler handler, DataChangeEvent event) {
		try (DataChangeScope ignored = DataChangeScope.suppress()) {
			if (handler.supports(event)) { handler.handle(event); }
		} catch (Exception cause) {
			failure(handler, event);
		}
	}

	private void failure(DataChangeHandler handler, DataChangeEvent event) {
		DataChangeException exception = new DataChangeException(DataChangeErrorCode.HANDLER_FAILED,
				"HANDLER_FAILED: " + handler.getClass().getName());
		if (event != null && event.getConsistencyLevel() == ConsistencyLevel.TRANSACTIONAL) {
			throw exception;
		}
		report(event, exception);
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

	private static final class OrderedHandler {
		private final DataChangeHandler handler;
		private final int order;

		private OrderedHandler(DataChangeHandler handler, int order) {
			this.handler = handler;
			this.order = order;
		}
	}
}
