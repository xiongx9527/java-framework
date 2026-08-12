package com.xiongx9527.fw.plus.tinyengine.support;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TinyEngineAiModelMigrationTest {

	@Test
	void aiModelMigrationCreatesIdempotentSchemaAndEnforcesUniqueConfigurations() throws Exception {
		JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_ai_model_config;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
		String migration = read("META-INF/tiny-engine/sql/h2/14_create_ai_model_config_ddl.sql");

		execute(jdbc, migration);
		execute(jdbc, migration);

		assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_name in "
				+ "('T_AI_MODEL_SERVICE', 'T_AI_MODEL')", Integer.class)).isEqualTo(2);
		assertThat(jdbc.queryForObject("select character_maximum_length from information_schema.columns "
				+ "where table_name = 'T_AI_MODEL_SERVICE' and column_name = 'API_KEY'", Integer.class)).isEqualTo(2048);
		assertThat(jdbc.queryForObject("select count(*) from information_schema.indexes where table_name = 'T_AI_MODEL' "
				+ "and index_name in ('IDX_AI_MODEL_TENANT_APP', 'IDX_AI_MODEL_SERVICE_ID')", Integer.class)).isEqualTo(2);

		insertService(jdbc, "openai");
		assertThatThrownBy(() -> insertService(jdbc, "openai"))
				.isInstanceOf(DataIntegrityViolationException.class);

		insertModel(jdbc, "gpt-4o");
		assertThatThrownBy(() -> insertModel(jdbc, "gpt-4o"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void insertService(JdbcTemplate jdbc, String serviceKey) {
		jdbc.update("insert into t_ai_model_service(tenant_id, app_id, service_key, provider, label, base_url) "
				+ "values ('tenant-a', 100, ?, 'openai', 'OpenAI', 'https://api.openai.com')", serviceKey);
	}

	private void insertModel(JdbcTemplate jdbc, String name) {
		Long serviceId = jdbc.queryForObject("select id from t_ai_model_service where service_key = 'openai'", Long.class);
		jdbc.update("insert into t_ai_model(tenant_id, app_id, service_id, name, label) values ('tenant-a', 100, ?, ?, ?)",
				serviceId, name, name);
	}

	private String read(String location) throws Exception {
		ClassPathResource resource = new ClassPathResource(location);
		try (InputStream inputStream = resource.getInputStream()) {
			return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		}
	}

	private void execute(JdbcTemplate jdbc, String script) {
		for (String statement : script.split(";")) {
			String sql = statement.trim();
			if (!sql.isEmpty()) {
				jdbc.execute(sql);
			}
		}
	}
}
