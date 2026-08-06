package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.seata.datasource;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import io.seata.rm.datasource.DataSourceProxy;
import io.seata.rm.datasource.SeataDataSourceProxy;
import io.seata.rm.datasource.util.JdbcUtils;
import io.seata.sqlparser.util.JdbcConstants;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

public class SeataDataSourceAdapter implements DbTxDataSourceAdapter {

	private static final String COORDINATOR = "seata";

	private static final Set<String> SUPPORTED_DATABASES = Collections.unmodifiableSet(
			new HashSet<String>(Arrays.asList(
					JdbcConstants.MYSQL,
					JdbcConstants.ORACLE,
					JdbcConstants.POSTGRESQL,
					JdbcConstants.SQLSERVER,
					JdbcConstants.MARIADB,
					JdbcConstants.POLARDBX,
					JdbcConstants.DM)));

	private final DataSourceProxyFactory proxyFactory;

	private final Set<DataSource> preparedDataSources = Collections.synchronizedSet(
			Collections.newSetFromMap(new IdentityHashMap<DataSource, Boolean>()));

	public SeataDataSourceAdapter() {
		this(new DataSourceProxyFactory() {
			@Override
			public DataSource create(DataSource dataSource) {
				return new DataSourceProxy(dataSource);
			}
		});
	}

	SeataDataSourceAdapter(DataSourceProxyFactory proxyFactory) {
		if (proxyFactory == null) {
			throw new IllegalArgumentException("DataSourceProxy factory must not be null");
		}
		this.proxyFactory = proxyFactory;
	}

	@Override
	public String getCoordinator() {
		return COORDINATOR;
	}

	@Override
	public DataSource prepare(String key, DataSource dataSource) {
		if (dataSource == null) {
			throw new IllegalArgumentException("DataSource must not be null: " + key);
		}
		String databaseType = databaseType(key, dataSource);
		if (!SUPPORTED_DATABASES.contains(databaseType)) {
			throw new IllegalStateException("DataSource " + key
					+ " uses a database unsupported by Seata AT: " + databaseType);
		}
		if (dataSource instanceof SeataDataSourceProxy) {
			preparedDataSources.add(dataSource);
			return dataSource;
		}
		DataSource prepared = proxyFactory.create(dataSource);
		if (!(prepared instanceof SeataDataSourceProxy)) {
			throw new IllegalStateException("Seata DataSourceProxy factory returned an invalid proxy: " + key);
		}
		preparedDataSources.add(prepared);
		return prepared;
	}

	@Override
	public void beforeRemove(DataSource dataSource) {
		if (preparedDataSources.contains(dataSource)) {
			throw new IllegalStateException(
					"A prepared Seata DataSource cannot be removed before restart");
		}
	}

	@Override
	public void close(DataSource dataSource) {
		// Seata 2.0 DataSourceProxy has no close/unregister contract. The wrapper owns the raw pool.
	}

	private static String databaseType(String key, DataSource dataSource) {
		if (dataSource instanceof DataSourceProxy) {
			return normalizeDatabaseType(key, ((DataSourceProxy) dataSource).getDbType());
		}
		if (dataSource instanceof SeataDataSourceProxy) {
			DataSource target = ((SeataDataSourceProxy) dataSource).getTargetDataSource();
			if (target == null || target == dataSource) {
				throw new IllegalStateException(
						"Failed to inspect the target DataSource for Seata AT: " + key);
			}
			return databaseType(key, target);
		}
		try (Connection connection = dataSource.getConnection()) {
			if (connection == null) {
				throw new SQLException("DataSource returned a null connection");
			}
			DatabaseMetaData metadata = connection.getMetaData();
			if (metadata == null || metadata.getURL() == null) {
				throw new SQLException("JDBC metadata URL is not available");
			}
			return normalizeDatabaseType(key, JdbcUtils.getDbType(metadata.getURL()));
		} catch (SQLException exception) {
			throw new IllegalStateException("Failed to inspect DataSource for Seata AT: " + key,
					exception);
		} catch (RuntimeException exception) {
			throw new IllegalStateException("Failed to identify the database for Seata AT: " + key,
					exception);
		}
	}

	private static String normalizeDatabaseType(String key, String databaseType) {
		if (databaseType == null || databaseType.trim().isEmpty()) {
			throw new IllegalStateException("Failed to identify the database for Seata AT: " + key);
		}
		return databaseType.trim().toLowerCase(Locale.ENGLISH);
	}

	@FunctionalInterface
	interface DataSourceProxyFactory {

		DataSource create(DataSource dataSource);
	}
}
