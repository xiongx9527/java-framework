package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

/**
 * 当前线程的动态数据源请求快照持有器。
 */
public final class DsRequestContextHolder {

	private static final ThreadLocal<DsRequestContext> CONTEXT = new ThreadLocal<>();

	private DsRequestContextHolder() {
	}

	public static DsRequestContext getContext() {
		return CONTEXT.get();
	}

	public static Scope open(DsRequestContext context) {
		return new Scope(context);
	}

	public static void clear() {
		CONTEXT.remove();
	}

	public static final class Scope implements AutoCloseable {

		private final DsRequestContext previous;
		private boolean closed;

		private Scope(DsRequestContext current) {
			this.previous = CONTEXT.get();
			if (current == null) {
				CONTEXT.remove();
			} else {
				CONTEXT.set(current);
			}
		}

		@Override
		public void close() {
			if (closed) {
				return;
			}
			closed = true;
			if (previous == null) {
				CONTEXT.remove();
			} else {
				CONTEXT.set(previous);
			}
		}

	}

}
