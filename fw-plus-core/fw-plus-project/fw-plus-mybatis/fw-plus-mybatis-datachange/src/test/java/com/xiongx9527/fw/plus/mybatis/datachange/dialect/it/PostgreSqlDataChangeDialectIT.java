package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.dialect.PostgreSqlDataChangeDialect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class PostgreSqlDataChangeDialectIT extends DialectDatabaseContract {
	PostgreSqlDataChangeDialectIT() { super("DATACHANGE_POSTGRES", new PostgreSqlDataChangeDialect()); }

	@Test
	void runsRealDatabaseContract() throws Exception { runContract(); }

	@Override protected boolean supportsCreatedSchema() { return true; }
	@Override protected String driverClassName() { return "org.postgresql.Driver"; }
	@Override protected String qualified(String schema, String table) { return '"' + schema + "\".\"" + table + '"'; }
	@Override protected String createTableSql(String table) {
		return "CREATE TABLE " + table + " (id BIGSERIAL UNIQUE, tenant_id BIGINT NOT NULL, "
				+ "order_no VARCHAR(40) NOT NULL, amount INTEGER NOT NULL, status VARCHAR(20) DEFAULT 'NEW', "
				+ "touched BIGINT DEFAULT 0, CONSTRAINT dc_event_pk PRIMARY KEY(tenant_id, order_no))";
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
		return "INSERT INTO " + table + " (tenant_id,order_no,amount) "
				+ "VALUES (#{tenant_id},#{order_no},#{amount}) "
				+ "ON CONFLICT ON CONSTRAINT dc_event_pk DO UPDATE SET amount=EXCLUDED.amount";
	}
	@Override protected String returningSql(String table) {
		return "INSERT INTO " + table + " (tenant_id,order_no,amount) "
				+ "VALUES (#{tenant_id},#{order_no},#{amount}) RETURNING id";
	}
}
