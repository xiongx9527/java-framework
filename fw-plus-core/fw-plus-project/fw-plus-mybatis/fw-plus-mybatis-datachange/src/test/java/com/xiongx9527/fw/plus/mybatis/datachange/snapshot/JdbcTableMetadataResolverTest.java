package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcTableMetadataResolverTest {

	@Test
	void sortsPrimaryKeysByJdbcKeySequence() throws Exception {
		Connection connection = connection("jdbc:h2:mem:orders", primaryKeys(), columns());
		JdbcTableMetadataResolver resolver = new JdbcTableMetadataResolver();

		JdbcTableMetadata metadata = resolver.resolve(connection,
				new TableIdentifier(null, "PUBLIC", "ORDERS"));

		assertThat(metadata.getKeyColumns()).containsExactly("TENANT_ID", "ORDER_NO");
		assertThat(metadata.getColumns()).containsExactly("TENANT_ID", "ORDER_NO", "TOTAL");
	}

	@Test
	void keepsMetadataCachesSeparateForDifferentDataSources() throws Exception {
		JdbcTableMetadataResolver resolver = new JdbcTableMetadataResolver();
		Connection first = connection("jdbc:h2:mem:first", primaryKeys(), columns());
		Connection second = connection("jdbc:h2:mem:second", primaryKeys(), columns());
		TableIdentifier table = new TableIdentifier(null, "PUBLIC", "ORDERS");

		assertThat(resolver.resolve(first, table)).isNotSameAs(resolver.resolve(second, table));
	}

	private Connection connection(String url, ResultSet keys, ResultSet columns) throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		DatabaseMetaData metadata = Mockito.mock(DatabaseMetaData.class);
		Mockito.when(connection.getMetaData()).thenReturn(metadata);
		Mockito.when(metadata.getURL()).thenReturn(url);
		Mockito.when(metadata.getDatabaseProductName()).thenReturn("H2");
		Mockito.when(metadata.getPrimaryKeys(null, "public", "orders")).thenReturn(keys);
		Mockito.when(metadata.getColumns(null, "public", "orders", null)).thenReturn(columns);
		return connection;
	}

	private ResultSet primaryKeys() throws Exception {
		ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.next()).thenReturn(true, true, false);
		Mockito.when(resultSet.getString("COLUMN_NAME")).thenReturn("ORDER_NO", "TENANT_ID");
		Mockito.when(resultSet.getShort("KEY_SEQ")).thenReturn((short) 2, (short) 1);
		return resultSet;
	}

	private ResultSet columns() throws Exception {
		ResultSet resultSet = Mockito.mock(ResultSet.class);
		Mockito.when(resultSet.next()).thenReturn(true, true, true, false);
		Mockito.when(resultSet.getString("COLUMN_NAME")).thenReturn("TENANT_ID", "ORDER_NO", "TOTAL");
		return resultSet;
	}
}
