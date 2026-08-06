package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.coordinator;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.GlobalStatus;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;
import io.seata.tm.api.GlobalTransactionRole;

public class DefaultSeataTransactionFactory implements SeataTransactionFactory {

	@Override
	public SeataTransaction getCurrentOrCreate() {
		return new GlobalTransactionAdapter(GlobalTransactionContext.getCurrentOrCreate());
	}

	private static final class GlobalTransactionAdapter implements SeataTransaction {

		private final GlobalTransaction transaction;

		private GlobalTransactionAdapter(GlobalTransaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public boolean isLauncher() {
			return transaction.getGlobalTransactionRole() == GlobalTransactionRole.Launcher;
		}

		@Override
		public void begin(int timeoutMs, String name) throws TransactionException {
			transaction.begin(timeoutMs, name);
		}

		@Override
		public void commit() throws TransactionException {
			transaction.commit();
		}

		@Override
		public void rollback() throws TransactionException {
			transaction.rollback();
		}

		@Override
		public String getXid() {
			return transaction.getXid();
		}

		@Override
		public GlobalStatus getGlobalStatus() {
			try {
				return transaction.getStatus();
			} catch (TransactionException ignored) {
				return transaction.getLocalStatus();
			}
		}
	}
}
