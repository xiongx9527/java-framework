package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.dialect.MariaDbDataChangeDialect;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

class MariaDbDataChangeDialectIT extends DialectDatabaseContract {
	MariaDbDataChangeDialectIT() { super("DATACHANGE_MARIADB", new MariaDbDataChangeDialect()); }

	@Test
	void runsRealDatabaseContract() throws Exception { runContract(); }

	@Override protected boolean supportsCreatedSchema() { return true; }
	@Override protected String driverClassName() { return "org.mariadb.jdbc.Driver"; }
	@Override protected String qualified(String schema, String table) { return '`' + schema + "`.`" + table + '`'; }
	@Override protected String dmlTable(String schema, String table) { return '`' + table + '`'; }
	@Override protected String monitorSchema(String schema) { return null; }
	@Override protected void configureConnection(Connection connection, String schema) throws Exception {
		connection.setCatalog(schema);
	}
	@Override protected String createTableSql(String table) {
		return "CREATE TABLE " + table + " (id BIGINT AUTO_INCREMENT, tenant_id BIGINT NOT NULL, "
				+ "order_no VARCHAR(40) NOT NULL, amount INT NOT NULL, status VARCHAR(20) DEFAULT 'NEW', "
				+ "touched BIGINT DEFAULT 0, PRIMARY KEY(tenant_id, order_no), UNIQUE KEY uk_id(id))";
	}
	@Override protected List<String> createTriggerSql(String schema, String table, String qualifiedTable) {
		return Collections.singletonList("CREATE TRIGGER `" + schema + "`.`trg_" + table
				+ "` BEFORE UPDATE ON " + qualifiedTable + " FOR EACH ROW SET NEW.touched = OLD.touched + 1");
	}
	@Override protected String upsertSql(String table) {
		return "INSERT INTO " + table + " (tenant_id,order_no,amount) "
				+ "VALUES (#{tenant_id},#{order_no},#{amount}) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)";
	}
	@Override protected int[] acceptedUpsertCounts() { return new int[]{1, 2}; }
}
