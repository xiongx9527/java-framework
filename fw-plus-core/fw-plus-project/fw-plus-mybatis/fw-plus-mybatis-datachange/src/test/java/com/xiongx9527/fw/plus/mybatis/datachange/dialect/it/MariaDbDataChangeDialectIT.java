package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.dialect.MariaDbDataChangeDialect;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

class MariaDbDataChangeDialectIT extends DialectDatabaseContract {
	MariaDbDataChangeDialectIT() { super("DATACHANGE_MARIADB", new MariaDbDataChangeDialect()); }

	@Test
	void runsRealDatabaseContract() throws Exception { runContract(); }

	@Override protected boolean supportsCreatedSchema() { return true; }
	@Override protected String qualified(String schema, String table) { return '`' + schema + "`.`" + table + '`'; }
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
		return "INSERT INTO " + table + " (id,tenant_id,order_no,amount) VALUES (?,?,?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)";
	}
	@Override protected String mergeSql(String table) {
		return "UPDATE " + table + " SET amount=? WHERE tenant_id=? AND order_no=?";
	}
	@Override protected int[] acceptedUpsertCounts() { return new int[]{1, 2}; }
}
