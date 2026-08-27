package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RowIdentity {
	private final Map<String, Object> keyValues;

	public RowIdentity(Map<String, Object> keyValues) {
		this.keyValues = new LinkedHashMap<String, Object>(keyValues == null ? Collections.<String, Object>emptyMap() : keyValues);
	}

	public Map<String, Object> getKeyValues() {
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(keyValues));
	}

	@Override
	public boolean equals(Object other) {
		return this == other || (other instanceof RowIdentity
				&& Objects.equals(keyValues, ((RowIdentity) other).keyValues));
	}

	@Override
	public int hashCode() { return Objects.hash(keyValues); }
}
