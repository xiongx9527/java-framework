package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;

import java.util.List;

public interface DataChangeDialect {
	boolean supports(DatabaseMetadata metadata);
	TableIdentifier normalize(TableIdentifier table);
	default List<AnalyzedStatement> analyze(BoundSqlContext context) {
		throw new UnsupportedOperationException("SQL analysis is not implemented by this dialect");
	}
	default SnapshotQuery buildBeforeQuery(ChangeTarget target) {
		throw new UnsupportedOperationException("Before snapshot query is not implemented by this dialect");
	}
	default SnapshotQuery buildAfterQuery(ChangeTarget target, List<RowIdentity> identities) {
		throw new UnsupportedOperationException("After snapshot query is not implemented by this dialect");
	}
	int getOrder();
}
