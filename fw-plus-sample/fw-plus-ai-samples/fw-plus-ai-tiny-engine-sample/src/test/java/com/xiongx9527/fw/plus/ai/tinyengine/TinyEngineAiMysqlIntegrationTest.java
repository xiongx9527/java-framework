package com.xiongx9527.fw.plus.ai.tinyengine;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldDecryptInterceptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldEncryptInterceptor;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelServiceConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelConfig;
import com.xiongx9527.fw.plus.tinyengine.model.entity.AiModelServiceConfig;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TinyEngineAiMysqlIntegrationTest {

	private static final String AES_KEY = "0123456789abcdef";

	@Test
	void mysql57PersistsEncryptedSharedModelsAndCleansThemByApplication() throws Exception {
		String url = System.getenv("TINY_ENGINE_MYSQL_TEST_URL");
		String username = System.getenv("TINY_ENGINE_MYSQL_TEST_USERNAME");
		String password = System.getenv("TINY_ENGINE_MYSQL_TEST_PASSWORD");
		Assumptions.assumeTrue(hasText(url) && hasText(username),
				"Set TINY_ENGINE_MYSQL_TEST_URL and TINY_ENGINE_MYSQL_TEST_USERNAME to run MySQL verification");

		DataSource dataSource = new PooledDataSource("com.mysql.cj.jdbc.Driver", url, username,
				password == null ? "" : password);
		executeAiModelMigrationTwice(dataSource);
		SqlSessionFactory sessionFactory = sessionFactory(dataSource);
		String suffix = Long.toString(System.currentTimeMillis());
		String tenantId = "comet-mysql-" + suffix;
		Integer appId = Integer.valueOf((int) (System.currentTimeMillis() % 1000000000L));
		String apiKey = "mysql-placeholder-key-" + suffix;
		String serviceKey = "openai-" + suffix;
		String modelName = "model-" + suffix;

		try {
			AiModelServiceConfig service = service(tenantId, appId, serviceKey, apiKey);
			AiModelConfig model;
			try (SqlSession browserOne = sessionFactory.openSession(true)) {
				AiModelServiceConfigMapper serviceMapper = browserOne.getMapper(AiModelServiceConfigMapper.class);
				AiModelConfigMapper modelMapper = browserOne.getMapper(AiModelConfigMapper.class);
				assertEquals(1, serviceMapper.insertService(service));
				assertNotNull(service.getId());
				model = model(service, modelName);
				assertEquals(1, modelMapper.insertModel(model));
				assertNotNull(model.getId());
			}

			String rawApiKey = rawApiKey(dataSource, tenantId, appId, service.getId());
			assertNotEquals(apiKey, rawApiKey);

			try (SqlSession browserTwo = sessionFactory.openSession()) {
				AiModelServiceConfig found = browserTwo.getMapper(AiModelServiceConfigMapper.class)
						.findService(tenantId, appId, service.getId());
				assertNotNull(found);
				assertEquals(apiKey, found.getApiKey());
				List<AiModelConfig> models = browserTwo.getMapper(AiModelConfigMapper.class)
						.findModels(tenantId, appId);
				assertEquals(1, models.size());
				assertEquals(modelName, models.get(0).getName());
			}

			AiModelServiceConfig duplicate = service(tenantId, appId, serviceKey, "duplicate-placeholder-key");
			assertThrows(RuntimeException.class, () -> {
				try (SqlSession duplicateWriter = sessionFactory.openSession(true)) {
					duplicateWriter.getMapper(AiModelServiceConfigMapper.class).insertService(duplicate);
				}
			});

			try (SqlSession cleanup = sessionFactory.openSession(true)) {
				assertEquals(1, cleanup.getMapper(AiModelConfigMapper.class).deleteByApp(tenantId, appId));
				assertEquals(1, cleanup.getMapper(AiModelServiceConfigMapper.class).deleteByApp(tenantId, appId));
			}
			assertEquals(0, rowCount(dataSource, "t_ai_model", tenantId, appId));
			assertEquals(0, rowCount(dataSource, "t_ai_model_service", tenantId, appId));
		} finally {
			deleteRows(dataSource, tenantId, appId);
		}
	}

	private void executeAiModelMigrationTwice(DataSource dataSource) throws Exception {
		ClassPathResource script = new ClassPathResource(
				"META-INF/tiny-engine/sql/mysql/12_create_ai_model_config_ddl.sql");
		try (Connection connection = dataSource.getConnection()) {
			ScriptUtils.executeSqlScript(connection, script);
			ScriptUtils.executeSqlScript(connection, script);
		}
	}

	private SqlSessionFactory sessionFactory(DataSource dataSource) throws Exception {
		Environment environment = new Environment("mysql-test", new JdbcTransactionFactory(), dataSource);
		MybatisConfiguration configuration = new MybatisConfiguration();
		configuration.setEnvironment(environment);
		configuration.addMapper(AiModelServiceConfigMapper.class);
		configuration.addMapper(AiModelConfigMapper.class);
		EncryptorProperties properties = new EncryptorProperties();
		properties.setPrivateKey(AES_KEY);
		configuration.addInterceptor(new FieldEncryptInterceptor(new DefaultEncryptor(), properties, null));
		configuration.addInterceptor(new FieldDecryptInterceptor(new DefaultEncryptor(), properties, null));
		parseMapper(configuration, "mapper/tinyengine/AiModelServiceConfigMapper.xml");
		parseMapper(configuration, "mapper/tinyengine/AiModelConfigMapper.xml");
		return new SqlSessionFactoryBuilder().build(configuration);
	}

	private void parseMapper(MybatisConfiguration configuration, String resource) throws Exception {
		InputStream input = Resources.getResourceAsStream(resource);
		try {
			new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
		} finally {
			input.close();
		}
	}

	private String rawApiKey(DataSource dataSource, String tenantId, Integer appId, Long serviceId) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"select api_key from t_ai_model_service where tenant_id = ? and app_id = ? and id = ?")) {
			statement.setString(1, tenantId);
			statement.setInt(2, appId.intValue());
			statement.setLong(3, serviceId.longValue());
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getString(1);
			}
		}
	}

	private int rowCount(DataSource dataSource, String table, String tenantId, Integer appId) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"select count(*) from " + table + " where tenant_id = ? and app_id = ?")) {
			statement.setString(1, tenantId);
			statement.setInt(2, appId.intValue());
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getInt(1);
			}
		}
	}

	private void deleteRows(DataSource dataSource, String tenantId, Integer appId) throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			try (PreparedStatement models = connection.prepareStatement(
					"delete from t_ai_model where tenant_id = ? and app_id = ?")) {
				models.setString(1, tenantId);
				models.setInt(2, appId.intValue());
				models.executeUpdate();
			}
			try (PreparedStatement services = connection.prepareStatement(
					"delete from t_ai_model_service where tenant_id = ? and app_id = ?")) {
				services.setString(1, tenantId);
				services.setInt(2, appId.intValue());
				services.executeUpdate();
			}
		}
	}

	private AiModelServiceConfig service(String tenantId, Integer appId, String serviceKey, String apiKey) {
		AiModelServiceConfig service = new AiModelServiceConfig();
		service.setTenantId(tenantId);
		service.setAppId(appId);
		service.setServiceKey(serviceKey);
		service.setProvider("openai");
		service.setLabel("MySQL OpenAI");
		service.setBaseUrl("https://example.invalid/v1");
		service.setApiKey(apiKey);
		service.setAllowEmptyApiKey(Boolean.FALSE);
		service.setEnabled(Boolean.TRUE);
		service.setSortOrder(Integer.valueOf(0));
		return service;
	}

	private AiModelConfig model(AiModelServiceConfig service, String name) {
		AiModelConfig model = new AiModelConfig();
		model.setTenantId(service.getTenantId());
		model.setAppId(service.getAppId());
		model.setServiceId(service.getId());
		model.setName(name);
		model.setLabel(name);
		model.setCapabilities("chat");
		model.setEnabled(Boolean.TRUE);
		model.setSortOrder(Integer.valueOf(0));
		return model;
	}

	private boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}
}
