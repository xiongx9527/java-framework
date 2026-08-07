package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
