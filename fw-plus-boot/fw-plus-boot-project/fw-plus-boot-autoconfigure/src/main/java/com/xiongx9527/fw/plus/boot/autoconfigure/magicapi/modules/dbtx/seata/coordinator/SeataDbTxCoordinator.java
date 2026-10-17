package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.seata.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCallback;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator.DbTxCoordinator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import org.apache.seata.core.model.GlobalStatus;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;

public class SeataDbTxCoordinator implements DbTxCoordinator {

	private static final String NAME = "seata";

	private final MagicDynamicDataSource dynamicDataSource;

	private final SeataTransactionFactory transactionFactory;

	public SeataDbTxCoordinator(MagicDynamicDataSource dynamicDataSource,
			SeataTransactionFactory transactionFactory) {
		if (dynamicDataSource == null) {
			throw new IllegalArgumentException("MagicDynamicDataSource must not be null");
		}
		if (transactionFactory == null) {
			throw new IllegalArgumentException("SeataTransactionFactory must not be null");
		}
		this.dynamicDataSource = dynamicDataSource;
		this.transactionFactory = transactionFactory;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Object execute(DbTxDefinition definition, DbTxCallback callback) throws Throwable {
		if (definition == null) {
			throw validation("Definition must not be null");
		}
		if (callback == null) {
			throw validation("Callback must not be null");
		}

		prepareParticipants(definition);
		SeataTransaction transaction = transaction();
		boolean launcher = transaction.isLauncher();
		if (launcher) {
			begin(transaction, definition);
		}

		Object result;
		try {
			result = callback.execute();
		} catch (Throwable callbackFailure) {
			if (launcher) {
				rollbackAndSuppress(transaction, callbackFailure);
			}
			throw callbackFailure;
		}

		DbTxContext context = DbTxContextHolder.current();
		if (context != null && context.isRollbackOnly()) {
			DbTxException rollbackOnly = new DbTxException(DbTxErrorCode.DBTX_ROLLBACK_ONLY,
					"The dbtx transaction is marked rollback-only");
			if (launcher) {
				rollbackAndSuppress(transaction, rollbackOnly);
			}
			throw rollbackOnly;
		}

		if (launcher) {
			commit(transaction);
		}
		return result;
	}

	private void prepareParticipants(DbTxDefinition definition) {
		for (String key : definition.getDataSourceKeys()) {
			try {
				MagicDynamicDataSource.DataSourceNode node = dynamicDataSource.getDataSource(key);
				DataSource dataSource = node.getDataSource();
				if (!(dataSource instanceof DbTxAwareDataSource)) {
					throw new IllegalStateException("DataSource is not dbtx-aware: " + key);
				}
				((DbTxAwareDataSource) dataSource).prepare(NAME);
			} catch (Throwable prepareFailure) {
				throw new DbTxException(DbTxErrorCode.DBTX_BEGIN_FAILED,
						"Failed to prepare Seata participant DataSource: " + key, prepareFailure);
			}
		}
	}

	private SeataTransaction transaction() {
		try {
			SeataTransaction transaction = transactionFactory.getCurrentOrCreate();
			if (transaction == null) {
				throw new IllegalStateException("SeataTransactionFactory returned null");
			}
			return transaction;
		} catch (Throwable failure) {
			throw new DbTxException(DbTxErrorCode.DBTX_BEGIN_FAILED,
					"Failed to obtain a Seata global transaction", failure);
		}
	}

	private static void begin(SeataTransaction transaction, DbTxDefinition definition) {
		try {
			transaction.begin(definition.getTimeoutMs(), definition.getName());
		} catch (Throwable beginFailure) {
			throw new DbTxException(DbTxErrorCode.DBTX_BEGIN_FAILED,
					"Failed to begin a Seata global transaction", beginFailure);
		}
	}

	private static void rollbackAndSuppress(SeataTransaction transaction, Throwable originalFailure) {
		try {
			transaction.rollback();
		} catch (Throwable rollbackFailure) {
			originalFailure.addSuppressed(rollbackFailure);
		}
	}

	private static void commit(SeataTransaction transaction) {
		try {
			transaction.commit();
		} catch (Throwable commitFailure) {
			String xid = bestXid(transaction);
			GlobalStatus status = bestStatus(transaction);
			throw new DbTxException(DbTxErrorCode.DBTX_COMMIT_FAILED,
					"Failed to commit Seata global transaction [xid=" + xid
							+ ", status=" + status + "]", commitFailure);
		}
	}

	private static String bestXid(SeataTransaction transaction) {
		try {
			String xid = transaction.getXid();
			return xid == null ? "unknown" : xid;
		} catch (Throwable ignored) {
			return "unknown";
		}
	}

	private static GlobalStatus bestStatus(SeataTransaction transaction) {
		try {
			GlobalStatus status = transaction.getGlobalStatus();
			return status == null ? GlobalStatus.UnKnown : status;
		} catch (Throwable ignored) {
			return GlobalStatus.UnKnown;
		}
	}

	private static DbTxException validation(String message) {
		return new DbTxException(DbTxErrorCode.DBTX_VALIDATION_FAILED, message);
	}
}
