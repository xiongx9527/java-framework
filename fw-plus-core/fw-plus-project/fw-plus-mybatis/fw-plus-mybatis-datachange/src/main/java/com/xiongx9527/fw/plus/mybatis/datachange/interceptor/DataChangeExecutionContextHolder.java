package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class DataChangeExecutionContextHolder {
	private static final ThreadLocal<Deque<DataChangeExecutionContext>> CONTEXTS =
			new ThreadLocal<Deque<DataChangeExecutionContext>>();

	private DataChangeExecutionContextHolder() {
	}

	public static Optional<DataChangeExecutionContext> current() {
		Deque<DataChangeExecutionContext> contexts = CONTEXTS.get();
		return contexts == null || contexts.isEmpty()
				? Optional.<DataChangeExecutionContext>empty() : Optional.of(contexts.peek());
	}

	public static Scope push(DataChangeExecutionContext context) {
		if (context == null) {
			throw new IllegalArgumentException("context must not be null");
		}
		Deque<DataChangeExecutionContext> contexts = CONTEXTS.get();
		if (contexts == null) {
			contexts = new ArrayDeque<DataChangeExecutionContext>();
			CONTEXTS.set(contexts);
		}
		contexts.push(context);
		return new Scope(context);
	}

	public static final class Scope implements AutoCloseable {
		private final DataChangeExecutionContext context;
		private boolean closed;

		private Scope(DataChangeExecutionContext context) { this.context = context; }

		@Override
		public void close() {
			if (closed) { return; }
			closed = true;
			Deque<DataChangeExecutionContext> contexts = CONTEXTS.get();
			if (contexts == null || contexts.isEmpty() || contexts.peek() != context) {
				CONTEXTS.remove();
				throw new IllegalStateException("Data change execution contexts must close in stack order");
			}
			contexts.pop();
			if (contexts.isEmpty()) { CONTEXTS.remove(); }
		}
	}
}
