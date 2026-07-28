package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

public class TriggerDropResult {
	private final boolean dropped;

	public TriggerDropResult(boolean dropped) {
		this.dropped = dropped;
	}

	public boolean isDropped() {
		return dropped;
	}
}
