package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

public final class BinlogPosition {
	private final long sequence;
	private final String storeId;

	public BinlogPosition(long sequence, String storeId) {
		if (isBlank(storeId)) {
			throw new IllegalArgumentException("Store id must not be blank.");
		}
		this.sequence = sequence;
		this.storeId = storeId;
	}

	public long getSequence() {
		return sequence;
	}

	public String getStoreId() {
		return storeId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof BinlogPosition)) {
			return false;
		}
		BinlogPosition that = (BinlogPosition) o;
		if (sequence != that.sequence) {
			return false;
		}
		return storeId.equals(that.storeId);
	}

	@Override
	public int hashCode() {
		int result = (int) (sequence ^ (sequence >>> 32));
		result = 31 * result + storeId.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return storeId + ":" + sequence;
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
