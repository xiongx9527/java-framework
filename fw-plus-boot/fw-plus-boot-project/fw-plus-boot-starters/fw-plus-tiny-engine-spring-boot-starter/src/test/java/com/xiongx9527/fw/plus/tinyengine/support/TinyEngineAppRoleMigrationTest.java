package com.xiongx9527.fw.plus.tinyengine.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TinyEngineAppRoleMigrationTest {

	@Test
	void appEnableAppCodeAndPermissionRoleCodeMigrationsBackfillAndEnforceConstraints() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_app_role_migration_starter;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("create table t_tenant (id int primary key, enable boolean)");
		jdbcTemplate.update("insert into t_tenant(id, enable) values (1, null), (2, false)");
		jdbcTemplate.execute("create table t_app (id int primary key, enable boolean, code varchar(255))");
		jdbcTemplate.update("insert into t_app(id, enable, code) values (1, null, null), (2, false, null)");
		jdbcTemplate.execute("create table t_permission_role (id int primary key, name varchar(255) not null, "
				+ "role_type varchar(60) not null, code varchar(255))");
		jdbcTemplate.update("insert into t_permission_role(id, name, role_type, code) values "
				+ "(1, 'Tinybuilder_Admin', 'system', null), (2, 'Custom Role', 'app', null)");

		ClassPathResource resource = new ClassPathResource(
				"META-INF/tiny-engine/sql/h2/05_update_all_tables_ddl.sql");
		String migration;
		try (InputStream inputStream = resource.getInputStream()) {
			migration = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		}
		for (String statement : migration.split(";")) {
			String sql = statement.trim();
			if (sql.isEmpty()) {
				continue;
			}
			boolean tenantStatement = (sql.startsWith("ALTER TABLE t_tenant") && sql.contains("enable"))
					|| sql.startsWith("UPDATE t_tenant SET enable");
			boolean appStatement = (sql.startsWith("ALTER TABLE t_app")
					&& (sql.contains("enable") || sql.contains("code")))
					|| sql.startsWith("UPDATE t_app SET enable")
					|| sql.startsWith("UPDATE t_app SET code")
					|| (sql.startsWith("CREATE UNIQUE INDEX") && sql.contains("app_code"));
			boolean roleStatement = (sql.startsWith("ALTER TABLE t_permission_role") && sql.contains("code"))
					|| sql.startsWith("UPDATE t_permission_role SET code")
					|| (sql.startsWith("CREATE UNIQUE INDEX") && sql.contains("permission_role_code"));
			if (tenantStatement || appStatement || roleStatement) {
				jdbcTemplate.execute(sql);
			}
		}

		assertThat(jdbcTemplate.queryForObject("select enable from t_tenant where id = 1", Boolean.class)).isTrue();
		assertThat(jdbcTemplate.queryForObject("select enable from t_tenant where id = 2", Boolean.class)).isFalse();
		assertThat(jdbcTemplate.queryForObject("select enable from t_app where id = 1", Boolean.class)).isTrue();
		assertThat(jdbcTemplate.queryForObject("select enable from t_app where id = 2", Boolean.class)).isFalse();
		assertThat(jdbcTemplate.queryForObject("select code from t_app where id = 1", String.class)).isEqualTo("app_1");
		assertThat(jdbcTemplate.queryForObject("select code from t_app where id = 2", String.class)).isEqualTo("app_2");
		assertThat(jdbcTemplate.queryForObject("select code from t_permission_role where id = 1",
				String.class)).isEqualTo("Tinybuilder_Admin");
		assertThat(jdbcTemplate.queryForObject("select code from t_permission_role where id = 2",
				String.class)).isEqualTo("role_2");
		assertThatThrownBy(() -> jdbcTemplate.update(
				"insert into t_tenant(id, enable) values (3, null)"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update(
				"insert into t_app(id, enable, code) values (3, null, null)"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update(
				"insert into t_app(id, enable, code) values (4, 1, 'app_1')"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update(
				"insert into t_permission_role(id, name, role_type, code) "
						+ "values (3, 'Duplicate', 'app', 'Tinybuilder_Admin')"))
				.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}
}
