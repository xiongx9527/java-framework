package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrameworkChangeMetadata {
	private final TableIdentifier table;
	private final List<String> keyColumns;
	private final LogicalDeleteMetadata logicalDelete;
	private final GeneratedKeyAccessor generatedKeyAccessor;

	public FrameworkChangeMetadata(TableIdentifier table, List<String> keyColumns,
			LogicalDeleteMetadata logicalDelete, GeneratedKeyAccessor generatedKeyAccessor) {
		this.table = table;
		this.keyColumns = Collections.unmodifiableList(new ArrayList<String>(
				keyColumns == null ? Collections.<String>emptyList() : keyColumns));
		this.logicalDelete = logicalDelete;
		this.generatedKeyAccessor = generatedKeyAccessor;
	}

	public TableIdentifier getTable() { return table; }
	public List<String> getKeyColumns() { return keyColumns; }
	public LogicalDeleteMetadata getLogicalDelete() { return logicalDelete; }
	public GeneratedKeyAccessor getGeneratedKeyAccessor() { return generatedKeyAccessor; }
}
