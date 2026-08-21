package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model;

public final class DbTxContextHolder {

	private static final ThreadLocal<DbTxContext> CONTEXT = new ThreadLocal<>();

	private DbTxContextHolder() {
	}

	public static DbTxContext current() {
		return CONTEXT.get();
	}

	public static DbTxContext open(DbTxDefinition definition) {
		if (CONTEXT.get() != null) {
			throw new IllegalStateException("A transaction context is already open");
		}
		DbTxContext context = new DbTxContext(definition);
		CONTEXT.set(context);
		return context;
	}

	public static void clear() {
		CONTEXT.remove();
	}
}
