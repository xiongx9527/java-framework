package com.xiongx9527.fw.plus.mybatis.datascope.helper;

import java.util.function.Supplier;

public final class RowPermissionHelper {
	private static final ThreadLocal<Status> STATUS = new ThreadLocal<Status>();

	private RowPermissionHelper() {
	}

	public static void setStatus(int status) {
		STATUS.set(new Status(status, true));
	}

	public static Status getStatus() {
		return STATUS.get();
	}

	public static void clearStatus() {
		STATUS.remove();
	}

	public static <T> T execUseStatus(int status, Supplier<T> callback) {
		STATUS.set(new Status(status, false));
		try {
			return callback.get();
		} finally {
			STATUS.remove();
		}
	}

	public static void execUseStatus(int status, Runnable callback) {
		STATUS.set(new Status(status, false));
		try {
			callback.run();
		} finally {
			STATUS.remove();
		}
	}

	public static final class Status {
		private final int status;
		private final boolean autoClearStatus;

		private Status(int status, boolean autoClearStatus) {
			this.status = status;
			this.autoClearStatus = autoClearStatus;
		}

		public int getStatus() {
			return status;
		}

		public boolean isAutoClearStatus() {
			return autoClearStatus;
		}
	}
}
