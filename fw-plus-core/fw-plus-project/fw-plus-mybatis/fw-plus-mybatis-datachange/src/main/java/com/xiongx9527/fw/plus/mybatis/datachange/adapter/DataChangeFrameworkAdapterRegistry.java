package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DataChangeFrameworkAdapterRegistry {
	private final List<DataChangeFrameworkAdapter> adapters;

	public DataChangeFrameworkAdapterRegistry(List<DataChangeFrameworkAdapter> adapters) {
		this.adapters = new ArrayList<DataChangeFrameworkAdapter>(
				adapters == null ? Collections.<DataChangeFrameworkAdapter>emptyList() : adapters);
		Collections.sort(this.adapters, new Comparator<DataChangeFrameworkAdapter>() {
			@Override
			public int compare(DataChangeFrameworkAdapter left, DataChangeFrameworkAdapter right) {
				return Integer.compare(right.getOrder(), left.getOrder());
			}
		});
	}

	public DataChangeFrameworkAdapter select(DataChangeInvocation invocation) {
		DataChangeFrameworkAdapter selected = null;
		for (DataChangeFrameworkAdapter adapter : adapters) {
			if (!adapter.supports(invocation)) {
				continue;
			}
			if (selected != null && selected.getOrder() == adapter.getOrder()) {
				throw new DataChangeException(DataChangeErrorCode.AMBIGUOUS_FRAMEWORK_ADAPTER,
						"AMBIGUOUS_FRAMEWORK_ADAPTER: " + selected + ", " + adapter);
			}
			if (selected == null) {
				selected = adapter;
			}
		}
		return selected;
	}
}
