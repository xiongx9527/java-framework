package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RowChange {
	private final DataChangeOperation operation;
	private final RowIdentity identity;
	private final List<FieldChange> fields;

	public RowChange(DataChangeOperation operation, RowIdentity identity, List<FieldChange> fields) {
		this.operation = operation;
		this.identity = identity;
		this.fields = fields == null ? new ArrayList<FieldChange>() : new ArrayList<FieldChange>(fields);
	}

	public DataChangeOperation getOperation() { return operation; }
	public RowIdentity getIdentity() { return identity; }
	public List<FieldChange> getFields() { return Collections.unmodifiableList(new ArrayList<FieldChange>(fields)); }
	public Map<String, Object> getBefore() { return values(true); }
	public Map<String, Object> getAfter() { return values(false); }

	private Map<String, Object> values(boolean before) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (FieldChange field : fields) {
			values.put(field.getColumnName(), before ? field.getBeforeValue() : field.getAfterValue());
		}
		return Collections.unmodifiableMap(values);
	}
}
