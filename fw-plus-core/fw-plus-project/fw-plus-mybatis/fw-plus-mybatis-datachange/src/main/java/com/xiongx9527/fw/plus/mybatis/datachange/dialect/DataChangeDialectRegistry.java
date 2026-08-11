package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
			throw new DataChangeException(DataChangeErrorCode.UNSUPPORTED_DIALECT,
					"No data change dialect supports " + metadata.getProductName());
		}
		cache.put(metadata, selected);
		return selected;
	}

	public List<AnalyzedStatement> analyze(Connection connection, BoundSqlContext context,
											 TableMonitorRegistry monitors) {
		SqlTargetDetection detection = new JSqlParserTargetDetector().detect(context.getSql());
		if (!detection.mightMatch(monitors)) {
			return Collections.emptyList();
		}
		DataChangeDialect dialect = select(connection);
		List<AnalyzedStatement> plan = dialect.analyze(context);
		List<AnalyzedStatement> filtered = new ArrayList<AnalyzedStatement>();
		for (AnalyzedStatement statement : plan) {
			List<ChangeTarget> targets = new ArrayList<ChangeTarget>();
			for (ChangeTarget target : statement.getTargets()) {
				Optional<TableMonitor> monitor = monitors.find(target.getTable(), dialect);
				if (!monitor.isPresent()) {
					continue;
				}
				TableMonitor configured = monitor.get();
				int maxRows = configured.getMaxSnapshotRows() == null
						? context.getDefaultMaxRows() : configured.getMaxSnapshotRows().intValue();
				targets.add(target.withSnapshotConfiguration(configured.getKeyColumns(),
						configured.getIncludeColumns(), maxRows, context.isLockRows()));
			}
			filtered.add(statement.withTargets(targets));
		}
		return Collections.unmodifiableList(filtered);
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
