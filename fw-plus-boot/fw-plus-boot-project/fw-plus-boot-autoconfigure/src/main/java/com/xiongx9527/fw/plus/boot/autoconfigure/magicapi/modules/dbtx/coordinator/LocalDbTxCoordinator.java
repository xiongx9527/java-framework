package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxErrorCode;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.exception.DbTxException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxOutcome;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalDbTxCoordinator implements DbTxCoordinator {

	private static final String NAME = "local";

	private static final String COMMIT_PHASE = "commit";

	private final MagicDynamicDataSource dynamicDataSource;

	public LocalDbTxCoordinator(MagicDynamicDataSource dynamicDataSource) {
		if (dynamicDataSource == null) {
			throw new IllegalArgumentException("MagicDynamicDataSource must not be null");
		}
		this.dynamicDataSource = dynamicDataSource;
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
		ensureNoAmbientTransaction();

		List<Participant> participants = participants(definition);
		List<DbTxAwareDataSource.Lease> leases = acquireLeases(participants);
		try {
			List<TransactionHandle> transactions = begin(definition, participants);
			Object result;
			try {
				result = callback.execute();
			} catch (Throwable callbackFailure) {
				rollbackAndSuppress(transactions, transactions.size() - 1, callbackFailure);
				throw callbackFailure;
			}

			DbTxContext context = DbTxContextHolder.current();
			if (context != null && context.isRollbackOnly()) {
				DbTxException rollbackOnly = new DbTxException(DbTxErrorCode.DBTX_ROLLBACK_ONLY,
						"The dbtx transaction is marked rollback-only");
				rollbackAndSuppress(transactions, transactions.size() - 1, rollbackOnly);
				throw rollbackOnly;
			}

			commit(transactions);
			return result;
		} finally {
			releaseLeases(leases);
		}
	}

	private List<Participant> participants(DbTxDefinition definition) {
		List<String> keys = new ArrayList<>(definition.getDataSourceKeys());
		Collections.sort(keys);
		List<Participant> participants = new ArrayList<>(keys.size());
		for (String key : keys) {
			try {
				MagicDynamicDataSource.DataSourceNode node = dynamicDataSource.getDataSource(key);
				participants.add(new Participant(key, node, dbTxDataSource(node.getDataSource())));
			} catch (Throwable resolveFailure) {
				throw new DbTxException(DbTxErrorCode.DBTX_BEGIN_FAILED,
						"Failed to resolve local transaction DataSource: " + key, resolveFailure);
			}
		}
		return participants;
	}

	private List<DbTxAwareDataSource.Lease> acquireLeases(List<Participant> participants) {
		List<DbTxAwareDataSource.Lease> leases = new ArrayList<DbTxAwareDataSource.Lease>();
		for (Participant participant : participants) {
			if (participant.dataSource == null) {
				continue;
			}
			try {
				leases.add(participant.dataSource.acquireLease());
			} catch (Throwable acquisitionFailure) {
				releaseLeases(leases);
				throw new DbTxException(DbTxErrorCode.DBTX_BEGIN_FAILED,
						"Failed to acquire local transaction DataSource: " + participant.key,
						acquisitionFailure);
			}
		}
		return leases;
	}

	private static DbTxAwareDataSource dbTxDataSource(DataSource dataSource) throws SQLException {
		if (dataSource instanceof DbTxAwareDataSource) {
			return (DbTxAwareDataSource) dataSource;
		}
		if (dataSource.isWrapperFor(DbTxAwareDataSource.class)) {
			return dataSource.unwrap(DbTxAwareDataSource.class);
		}
		return null;
	}

	private static void releaseLeases(List<DbTxAwareDataSource.Lease> leases) {
		for (int index = leases.size() - 1; index >= 0; index--) {
			leases.get(index).close();
		}
	}

	private List<TransactionHandle> begin(DbTxDefinition definition, List<Participant> participants) {
		List<TransactionHandle> transactions = new ArrayList<>(participants.size());
		for (Participant participant : participants) {
			try {
				DataSourceTransactionManager manager = participant.node.getDataSourceTransactionManager();
				TransactionStatus status = manager.getTransaction(transactionDefinition(definition));
				transactions.add(new TransactionHandle(participant.key, manager, status));
			} catch (Throwable beginFailure) {
				DbTxException exception = new DbTxException(DbTxErrorCode.DBTX_BEGIN_FAILED,
						"Failed to begin local transaction for DataSource: " + participant.key, beginFailure);
				rollbackAndSuppress(transactions, transactions.size() - 1, exception);
				throw exception;
			}
		}
		return transactions;
	}

	private static final class Participant {

		private final String key;

		private final MagicDynamicDataSource.DataSourceNode node;

		private final DbTxAwareDataSource dataSource;

		private Participant(String key, MagicDynamicDataSource.DataSourceNode node,
				DbTxAwareDataSource dataSource) {
			this.key = key;
			this.node = node;
			this.dataSource = dataSource;
		}
	}

	private void commit(List<TransactionHandle> transactions) {
		List<String> committed = new ArrayList<>();
		for (int index = transactions.size() - 1; index >= 0; index--) {
			TransactionHandle transaction = transactions.get(index);
			try {
				transaction.manager.commit(transaction.status);
				committed.add(transaction.key);
			} catch (Throwable commitFailure) {
				RollbackResult rollbackResult = rollback(transactions, index - 1);
				DbTxOutcome outcome = new DbTxOutcome(COMMIT_PHASE, NAME, transaction.key,
						committed, rollbackResult.rolledBack, rollbackResult.rollbackFailed,
						Collections.singletonList(transaction.key));
				DbTxErrorCode code = committed.isEmpty()
						? DbTxErrorCode.DBTX_COMMIT_FAILED : DbTxErrorCode.DBTX_HEURISTIC_MIXED;
				DbTxException exception = new DbTxException(code,
						"Failed to commit local transaction for DataSource: " + transaction.key,
						commitFailure, outcome);
				for (Throwable rollbackFailure : rollbackResult.failures) {
					exception.addSuppressed(rollbackFailure);
				}
				throw exception;
			}
		}
	}

	private void ensureNoAmbientTransaction() {
		if (TransactionSynchronizationManager.isActualTransactionActive()
				|| TransactionSynchronizationManager.isSynchronizationActive()
				|| !TransactionSynchronizationManager.getResourceMap().isEmpty()) {
			throw new DbTxException(DbTxErrorCode.DBTX_AMBIENT_TRANSACTION,
					"Cannot start dbtx inside an ambient Spring transaction");
		}
	}

	private static TransactionDefinition transactionDefinition(DbTxDefinition definition) {
		DefaultTransactionDefinition springDefinition = new DefaultTransactionDefinition();
		springDefinition.setName(definition.getName());
		if (definition.getTimeoutMs() > 0) {
			long timeoutSeconds = Math.max(1L, (definition.getTimeoutMs() + 999L) / 1000L);
			springDefinition.setTimeout((int) Math.min(Integer.MAX_VALUE, timeoutSeconds));
		}
		return springDefinition;
	}

	private static void rollbackAndSuppress(List<TransactionHandle> transactions, int lastIndex,
			Throwable originalFailure) {
		RollbackResult result = rollback(transactions, lastIndex);
		for (Throwable rollbackFailure : result.failures) {
			originalFailure.addSuppressed(rollbackFailure);
		}
	}

	private static RollbackResult rollback(List<TransactionHandle> transactions, int lastIndex) {
		RollbackResult result = new RollbackResult();
		for (int index = lastIndex; index >= 0; index--) {
			TransactionHandle transaction = transactions.get(index);
			try {
				transaction.manager.rollback(transaction.status);
				result.rolledBack.add(transaction.key);
			} catch (Throwable rollbackFailure) {
				result.rollbackFailed.add(transaction.key);
				result.failures.add(rollbackFailure);
			}
		}
		return result;
	}

	private static DbTxException validation(String message) {
		return new DbTxException(DbTxErrorCode.DBTX_VALIDATION_FAILED, message);
	}

	private static final class TransactionHandle {

		private final String key;

		private final DataSourceTransactionManager manager;

		private final TransactionStatus status;

		private TransactionHandle(String key, DataSourceTransactionManager manager,
				TransactionStatus status) {
			this.key = key;
			this.manager = manager;
			this.status = status;
		}
	}

	private static final class RollbackResult {

		private final List<String> rolledBack = new ArrayList<>();

		private final List<String> rollbackFailed = new ArrayList<>();

		private final List<Throwable> failures = new ArrayList<>();
	}
}
