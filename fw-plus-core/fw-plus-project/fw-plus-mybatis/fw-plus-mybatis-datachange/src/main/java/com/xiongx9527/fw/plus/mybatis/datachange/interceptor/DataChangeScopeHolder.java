package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

public final class DataChangeScopeHolder {
	private static final ThreadLocal<Integer> SUPPRESSION_DEPTH = new ThreadLocal<Integer>();

	private DataChangeScopeHolder() {
	}

	public static boolean isSuppressed() {
		Integer depth = SUPPRESSION_DEPTH.get();
		return depth != null && depth.intValue() > 0;
	}

	static void enter() {
		Integer depth = SUPPRESSION_DEPTH.get();
		SUPPRESSION_DEPTH.set(Integer.valueOf(depth == null ? 1 : depth.intValue() + 1));
	}

	static void exit() {
		Integer depth = SUPPRESSION_DEPTH.get();
		if (depth == null || depth.intValue() <= 1) {
			SUPPRESSION_DEPTH.remove();
		} else {
			SUPPRESSION_DEPTH.set(Integer.valueOf(depth.intValue() - 1));
		}
	}
}
