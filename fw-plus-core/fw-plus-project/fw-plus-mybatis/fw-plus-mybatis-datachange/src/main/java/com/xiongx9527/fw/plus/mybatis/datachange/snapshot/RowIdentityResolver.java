package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class RowIdentityResolver {

	public List<String> resolve(TableMonitor monitor, FrameworkChangeMetadata framework,
			JdbcTableMetadata jdbcMetadata) {
		List<String> selected = monitor == null ? Collections.<String>emptyList() : monitor.getKeyColumns();
		if (selected.isEmpty() && framework != null) {
			selected = framework.getKeyColumns();
		}
		if (selected.isEmpty() && jdbcMetadata != null) {
			selected = jdbcMetadata.getKeyColumns();
		}
		if (selected.isEmpty()) {
			throw new DataChangeException(DataChangeErrorCode.UNIQUE_KEY_UNAVAILABLE,
					"UNIQUE_KEY_UNAVAILABLE");
		}
		Set<String> seen = new HashSet<String>();
		List<String> keys = new ArrayList<String>();
		for (String key : selected) {
			String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ENGLISH);
			if (normalized.isEmpty() || !seen.add(normalized)) {
				throw new DataChangeException(DataChangeErrorCode.INVALID_UNIQUE_KEY,
						"INVALID_UNIQUE_KEY");
			}
			keys.add(key);
		}
		return Collections.unmodifiableList(keys);
	}
}
