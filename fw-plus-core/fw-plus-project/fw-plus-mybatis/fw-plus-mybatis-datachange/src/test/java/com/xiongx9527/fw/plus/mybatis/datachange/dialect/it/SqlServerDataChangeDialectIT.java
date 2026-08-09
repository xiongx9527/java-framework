package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SqlServerDataChangeDialect;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class SqlServerDataChangeDialectIT extends DialectDatabaseContract {
	SqlServerDataChangeDialectIT() { super("DATACHANGE_SQLSERVER", new SqlServerDataChangeDialect()); }

	@Test
	void runsRealDatabaseContract() throws Exception { runContract(); }

	@Override protected boolean supportsCreatedSchema() { return true; }
	@Override protected String driverClassName() { return "com.microsoft.sqlserver.jdbc.SQLServerDriver"; }
	@Override protected String qualified(String schema, String table) { return schema + "." + table; }
	@Override protected String createTableSql(String table) {
		return "CREATE TABLE " + table + " (id BIGINT IDENTITY(1,1) UNIQUE, tenant_id BIGINT NOT NULL, "
				+ "order_no VARCHAR(40) NOT NULL, amount INT NOT NULL, status VARCHAR(20) DEFAULT 'NEW', "
				+ "touched BIGINT DEFAULT 0, PRIMARY KEY(tenant_id, order_no))";
	}
	@Override protected List<String> createTriggerSql(String schema, String table, String qualifiedTable) {
		return Collections.singletonList("CREATE TRIGGER [" + schema + "].[trg_" + table + "] ON "
				+ qualifiedTable + " AFTER UPDATE AS BEGIN SET NOCOUNT ON; UPDATE target SET touched=deleted.touched+1 "
				+ "FROM " + qualifiedTable + " target JOIN inserted ON target.tenant_id=inserted.tenant_id "
				+ "AND target.order_no=inserted.order_no JOIN deleted ON target.tenant_id=deleted.tenant_id "
				+ "AND target.order_no=deleted.order_no; END");
	}
	@Override protected String upsertSql(String table) {
		return "MERGE INTO " + table + " target USING (SELECT CAST(#{tenant_id} AS BIGINT) AS tenant_id, "
				+ "CAST(#{order_no} AS VARCHAR(40)) AS order_no, CAST(#{amount} AS INT) AS amount) source "
				+ "ON (target.tenant_id=source.tenant_id AND target.order_no=source.order_no) "
				+ "WHEN MATCHED THEN UPDATE SET amount=source.amount "
				+ "WHEN NOT MATCHED THEN INSERT (tenant_id,order_no,amount) "
				+ "VALUES (source.tenant_id,source.order_no,source.amount);";
	}
}
