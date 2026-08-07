package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.dialect.PostgreSqlDataChangeDialect;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgreSqlDataChangeDialectIT extends DialectDatabaseContract {
	PostgreSqlDataChangeDialectIT() { super("DATACHANGE_POSTGRES", new PostgreSqlDataChangeDialect()); }

	@Test
	void runsRealDatabaseContract() throws Exception { runContract(); }

	@Override protected boolean supportsCreatedSchema() { return true; }
	@Override protected String qualified(String schema, String table) { return '"' + schema + "\".\"" + table + '"'; }
	@Override protected String createTableSql(String table) {
		return "CREATE TABLE " + table + " (id BIGSERIAL UNIQUE, tenant_id BIGINT NOT NULL, "
				+ "order_no VARCHAR(40) NOT NULL, amount INTEGER NOT NULL, status VARCHAR(20) DEFAULT 'NEW', "
				+ "touched BIGINT DEFAULT 0, PRIMARY KEY(tenant_id, order_no))";
	}
	@Override protected List<String> createTriggerSql(String schema, String table, String qualifiedTable) {
		String function = '"' + schema + "\".\"fn_" + table + '"';
		return Arrays.asList(
				"CREATE FUNCTION " + function + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
						+ "NEW.touched := OLD.touched + 1; RETURN NEW; END $$",
				"CREATE TRIGGER \"trg_" + table + "\" BEFORE UPDATE ON " + qualifiedTable
						+ " FOR EACH ROW EXECUTE FUNCTION " + function + "()");
	}
	@Override protected List<String> dropExtraSql(String schema, String table, String qualifiedTable) {
		return Arrays.asList(
				"DROP TRIGGER IF EXISTS \"trg_" + table + "\" ON " + qualifiedTable,
				"DROP FUNCTION IF EXISTS \"" + schema + "\".\"fn_" + table + "\"()");
	}
	@Override protected String upsertSql(String table) {
		return "INSERT INTO " + table + " (id,tenant_id,order_no,amount) VALUES (?,?,?,?) "
				+ "ON CONFLICT (id) DO UPDATE SET amount=EXCLUDED.amount";
	}
	@Override protected String mergeSql(String table) {
		return "UPDATE " + table + " SET amount=? WHERE tenant_id=? AND order_no=?";
	}
	@Override protected void verifyReturning(Connection connection, String table) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table
				+ " (tenant_id,order_no,amount) VALUES (?,?,?) RETURNING id")) {
			statement.setLong(1, 8L);
			statement.setString(2, "RETURNING");
			statement.setInt(3, 11);
			try (ResultSet rows = statement.executeQuery()) {
				assertThat(rows.next()).isTrue();
				assertThat(rows.getLong(1)).isPositive();
				assertThat(rows.next()).isFalse();
			}
		}
	}
}
