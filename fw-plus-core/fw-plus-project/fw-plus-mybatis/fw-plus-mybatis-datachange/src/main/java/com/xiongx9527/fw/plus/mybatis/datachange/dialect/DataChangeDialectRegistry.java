package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DataChangeDialectRegistry {
	private final List<DataChangeDialect> dialects;
	private final Map<DatabaseMetadata, DataChangeDialect> cache = new HashMap<DatabaseMetadata, DataChangeDialect>();

	public DataChangeDialectRegistry(List<DataChangeDialect> dialects) {
		this.dialects = new ArrayList<DataChangeDialect>(
				dialects == null ? Collections.<DataChangeDialect>emptyList() : dialects);
		Collections.sort(this.dialects, new Comparator<DataChangeDialect>() {
			@Override
			public int compare(DataChangeDialect left, DataChangeDialect right) {
				return Integer.compare(right.getOrder(), left.getOrder());
			}
		});
	}

	public synchronized DataChangeDialect select(Connection connection) {
		DatabaseMetadata metadata = metadata(connection);
		DataChangeDialect selected = cache.get(metadata);
		if (selected != null) {
			return selected;
		}
		for (DataChangeDialect dialect : dialects) {
			if (!dialect.supports(metadata)) {
				continue;
			}
			if (selected != null && selected.getOrder() == dialect.getOrder()) {
				throw new DataChangeException(DataChangeErrorCode.AMBIGUOUS_DIALECT,
						"AMBIGUOUS_DIALECT: " + selected + ", " + dialect);
			}
			if (selected == null) {
				selected = dialect;
			}
		}
		if (selected == null) {
			throw new DataChangeException(DataChangeErrorCode.CONFIGURATION,
					"No data change dialect supports " + metadata.getProductName());
		}
		cache.put(metadata, selected);
		return selected;
	}

	private DatabaseMetadata metadata(Connection connection) {
		try {
			DatabaseMetaData metadata = connection.getMetaData();
			return new DatabaseMetadata(metadata.getDatabaseProductName(), metadata.getDatabaseMajorVersion(),
					metadata.getDatabaseMinorVersion());
		} catch (SQLException exception) {
			throw new DataChangeException(DataChangeErrorCode.CONFIGURATION,
					"Unable to read database metadata", exception);
		}
	}
}
