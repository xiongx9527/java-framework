package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.summary;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 {@link SummaryStore} 默认实现（无需数据库，开箱可用；多实例部署时应换成共享存储实现）。
 */
public class InMemorySummaryStore implements SummaryStore {

	private final Map<String, SummaryRecord> records = new ConcurrentHashMap<String, SummaryRecord>();

	@Override
	public SummaryRecord get(String sessionKey) {
		if (sessionKey == null) {
			return SummaryRecord.EMPTY;
		}
		SummaryRecord record = records.get(sessionKey);
		return record == null ? SummaryRecord.EMPTY : record;
	}

	@Override
	public void put(String sessionKey, SummaryRecord record) {
		if (sessionKey == null || sessionKey.trim().isEmpty()) {
			return;
		}
		records.put(sessionKey, record == null ? SummaryRecord.EMPTY : record);
	}

	@Override
	public void remove(String sessionKey) {
		if (sessionKey != null) {
			records.remove(sessionKey);
		}
	}

}