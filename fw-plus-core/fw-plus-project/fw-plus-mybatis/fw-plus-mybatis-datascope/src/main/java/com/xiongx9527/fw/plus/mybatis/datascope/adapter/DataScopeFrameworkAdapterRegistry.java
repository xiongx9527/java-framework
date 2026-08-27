package com.xiongx9527.fw.plus.mybatis.datascope.adapter;

import org.apache.ibatis.mapping.MappedStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DataScopeFrameworkAdapterRegistry {
	private final List<DataScopeFrameworkAdapter> adapters;

	public DataScopeFrameworkAdapterRegistry(List<DataScopeFrameworkAdapter> adapters) {
		List<DataScopeFrameworkAdapter> selected = new ArrayList<DataScopeFrameworkAdapter>();
		if (adapters != null) {
			selected.addAll(adapters);
		}
		selected.add(new MybatisDataScopeAdapter());
		Collections.sort(selected, new Comparator<DataScopeFrameworkAdapter>() {
			@Override
			public int compare(DataScopeFrameworkAdapter left, DataScopeFrameworkAdapter right) {
				return Integer.compare(right.getOrder(), left.getOrder());
			}
		});
		this.adapters = Collections.unmodifiableList(selected);
	}

	public DataScopeFrameworkAdapter select(MappedStatement mappedStatement, Object parameterObject) {
		for (DataScopeFrameworkAdapter adapter : adapters) {
			if (adapter.supports(mappedStatement, parameterObject)) {
				return adapter;
			}
		}
		return null;
	}

	public List<DataScopeFrameworkAdapter> getAdapters() {
		return adapters;
	}
}
