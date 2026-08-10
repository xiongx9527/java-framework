package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.ChangeTarget;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotQuery;
import org.apache.ibatis.session.Configuration;

import java.sql.Connection;
import java.util.List;

public final class SnapshotService {
	private final JdbcSnapshotExecutor executor;

	public SnapshotService() {
		this.executor = new JdbcSnapshotExecutor();
	}

	public List<RowSnapshot> readBefore(Connection connection, DataChangeDialect dialect, ChangeTarget target,
			JdbcTableMetadata metadata, Configuration configuration) {
		return read(connection, dialect.buildBeforeQuery(target), target, metadata, configuration);
	}

	public List<RowSnapshot> readAfter(Connection connection, DataChangeDialect dialect, ChangeTarget target,
			List<RowIdentity> identities, JdbcTableMetadata metadata, Configuration configuration) {
		return read(connection, dialect.buildAfterQuery(target, identities), target, metadata, configuration);
	}

	private List<RowSnapshot> read(Connection connection, SnapshotQuery query, ChangeTarget target,
			JdbcTableMetadata metadata, Configuration configuration) {
		JdbcTableMetadata effective = new JdbcTableMetadata(metadata.getTable(), target.getKeyColumnsHint(),
				metadata.getColumns());
		return executor.query(connection, query, effective, configuration, target.getMaxRows());
	}
}
