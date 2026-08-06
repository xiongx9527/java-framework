package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class DbTxDataSourceDecorator implements MagicDataSourceDecorator {

	private final Collection<? extends DbTxDataSourceAdapter> adapters;

	public DbTxDataSourceDecorator(Collection<? extends DbTxDataSourceAdapter> adapters) {
		this.adapters = adapters == null ? Collections.<DbTxDataSourceAdapter>emptyList() : adapters;
	}

	@Override
	public DataSource decorate(String key, DataSource dataSource) {
		if (dataSource instanceof DbTxAwareDataSource) {
			return dataSource;
		}
		return new DbTxAwareDataSource(key, dataSource, adapters);
	}

	@Override
	public void beforeRemove(String key, DataSource dataSource) {
		if (dataSource instanceof DbTxAwareDataSource) {
			try {
				((DbTxAwareDataSource) dataSource).close();
			} catch (IOException e) {
				throw new IllegalStateException("Failed to close DataSource before removal: " + key, e);
			}
		}
	}
}
