package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.ChangeTarget;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatementChangePlan {
	private final int statementIndex;
	private final ChangeTarget target;
	private final TableMonitor monitor;
	private final JdbcTableMetadata tableMetadata;
	private final FrameworkChangeMetadata frameworkMetadata;
	private final DataChangeDialect dialect;
	private final List<RowSnapshot> beforeRows;
	private List<RowIdentity> identities = Collections.emptyList();
	private int updateCount = -1;

	public StatementChangePlan(int statementIndex, ChangeTarget target, TableMonitor monitor,
			JdbcTableMetadata tableMetadata, FrameworkChangeMetadata frameworkMetadata,
			DataChangeDialect dialect, List<RowSnapshot> beforeRows) {
		this.statementIndex = statementIndex;
		this.target = target;
		this.monitor = monitor;
		this.tableMetadata = tableMetadata;
		this.frameworkMetadata = frameworkMetadata;
		this.dialect = dialect;
		this.beforeRows = immutable(beforeRows);
	}

	public int getStatementIndex() { return statementIndex; }
	public ChangeTarget getTarget() { return target; }
	public TableMonitor getMonitor() { return monitor; }
	public JdbcTableMetadata getTableMetadata() { return tableMetadata; }
	public FrameworkChangeMetadata getFrameworkMetadata() { return frameworkMetadata; }
	public DataChangeDialect getDialect() { return dialect; }
	public List<RowSnapshot> getBeforeRows() { return beforeRows; }
	public List<RowIdentity> getIdentities() { return identities; }
	public int getUpdateCount() { return updateCount; }

	void captureResult(int count, List<RowIdentity> capturedIdentities) {
		updateCount = count;
		identities = immutable(capturedIdentities);
	}

	private <T> List<T> immutable(List<T> values) {
		return Collections.unmodifiableList(new ArrayList<T>(
				values == null ? Collections.<T>emptyList() : values));
	}
}
