package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

public final class DataChangeScope implements AutoCloseable {
	private boolean closed;

	private DataChangeScope() {
		DataChangeScopeHolder.enter();
	}

	public static DataChangeScope suppress() {
		return new DataChangeScope();
	}

	@Override
	public void close() {
		if (!closed) {
			closed = true;
			DataChangeScopeHolder.exit();
		}
	}
}
