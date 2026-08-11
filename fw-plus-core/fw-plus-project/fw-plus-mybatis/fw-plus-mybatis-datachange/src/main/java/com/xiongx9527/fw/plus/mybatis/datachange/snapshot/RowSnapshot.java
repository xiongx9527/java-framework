package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RowSnapshot {
	private final Map<String, Object> keyValues;
	private final Map<String, Object> values;

	public RowSnapshot(Map<String, Object> keyValues, Map<String, Object> values) {
		this.keyValues = copy(keyValues);
		this.values = copy(values);
	}

	public RowIdentity getIdentity() {
		return new RowIdentity(keyValues);
	}

	public Map<String, Object> getKeyValues() {
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(keyValues));
	}

	public Map<String, Object> getValues() {
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(values));
	}

	private Map<String, Object> copy(Map<String, Object> source) {
		return new LinkedHashMap<String, Object>(source == null
				? Collections.<String, Object>emptyMap() : source);
	}
}
