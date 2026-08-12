package com.xiongx9527.fw.plus.tinyengine.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.common.encrypt.DefaultEncryptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.config.EncryptorProperties;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldDecryptInterceptor;
import com.xiongx9527.fw.plus.mybatis.encrypt.interceptor.FieldEncryptInterceptor;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.mapper.AiModelServiceConfigMapper;
import com.xiongx9527.fw.plus.tinyengine.model.dto.AiModelServiceDto;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiModelEncryptedPersistenceTest {

	private static final String AES_KEY = "0123456789abcdef";
	private static SqlSessionFactory sessionFactory;
	private static DataSource dataSource;

	@BeforeAll
	static void setUp() throws Exception {
		dataSource = new PooledDataSource("org.h2.Driver", "jdbc:h2:mem:tinyengine-ai-model;DB_CLOSE_DELAY=-1", "sa", "");
		Environment environment = new Environment("test", new JdbcTransactionFactory(), dataSource);
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
		sessionFactory = new SqlSessionFactoryBuilder().build(configuration);

		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("create table t_ai_model_service (id bigint auto_increment primary key, tenant_id varchar(64) not null, app_id int not null, service_key varchar(64) not null, provider varchar(64) not null, label varchar(128) not null, base_url varchar(1024) not null, api_key varchar(2048), allow_empty_api_key boolean not null, enabled boolean not null, sort_order int not null, created_by varchar(64), created_time timestamp, last_updated_by varchar(64), last_updated_time timestamp)");
			statement.execute("create table t_ai_model (id bigint auto_increment primary key, tenant_id varchar(64) not null, app_id int not null, service_id bigint not null, name varchar(128) not null, label varchar(128) not null, capabilities varchar(2048), enabled boolean not null, sort_order int not null, created_by varchar(64), created_time timestamp, last_updated_by varchar(64), last_updated_time timestamp)");
		}
	}

	@Test
	void shouldEncryptApiKeyAtRestAndKeepItOutOfServiceDto() throws Exception {
		AiModelServiceConfig service = service("tenant-a", 2, "test-api-key");
		try (SqlSession session = sessionFactory.openSession(true)) {
			session.getMapper(AiModelServiceConfigMapper.class).insertService(service);
		}

		String raw = readRawApiKey(service.getId());
		assertNotEquals("test-api-key", raw);

		AiModelServiceConfig found;
		try (SqlSession session = sessionFactory.openSession()) {
			found = session.getMapper(AiModelServiceConfigMapper.class).findService("tenant-a", 2, service.getId());
		}
		assertNotNull(found);
		assertEquals("test-api-key", found.getApiKey());
		String json = JSON.toJSONString(AiModelServiceDto.from(service));
		assertFalse(json.contains("test-api-key"));
		assertFalse(json.contains(raw));
	}

	@Test
	void shouldScopeModelQueriesToTenantAndApplication() {
		AiModelServiceConfig service = service("tenant-b", 3, "another-key");
		try (SqlSession session = sessionFactory.openSession(true)) {
			AiModelServiceConfigMapper serviceMapper = session.getMapper(AiModelServiceConfigMapper.class);
			AiModelConfigMapper modelMapper = session.getMapper(AiModelConfigMapper.class);
			serviceMapper.insertService(service);
			modelMapper.insertModel(model(service, "gpt-test"));
		}

		try (SqlSession session = sessionFactory.openSession()) {
			List<AiModelConfig> models = session.getMapper(AiModelConfigMapper.class).findModels("tenant-b", 3);
			assertEquals(1, models.size());
			assertEquals("gpt-test", models.get(0).getName());
			assertEquals(0, session.getMapper(AiModelConfigMapper.class).findModels("tenant-a", 3).size());
		}
	}

	@Test
	void shouldRoundTripGeneratedIdsBeyondIntegerRange() throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("alter table t_ai_model_service alter column id restart with 2147483648");
			statement.execute("alter table t_ai_model alter column id restart with 2147483648");
		}
		AiModelServiceConfig service = service("tenant-large", 9, "large-id-key");
		AiModelConfig model;
		try (SqlSession session = sessionFactory.openSession(true)) {
			AiModelServiceConfigMapper serviceMapper = session.getMapper(AiModelServiceConfigMapper.class);
			AiModelConfigMapper modelMapper = session.getMapper(AiModelConfigMapper.class);
			serviceMapper.insertService(service);
			model = model(service, "large-id-model");
			modelMapper.insertModel(model);
		}

		assertEquals(Long.valueOf(2147483648L), service.getId());
		assertEquals(Long.valueOf(2147483648L), model.getId());
		try (SqlSession session = sessionFactory.openSession()) {
			assertEquals(service.getId(), session.getMapper(AiModelServiceConfigMapper.class)
					.findService("tenant-large", 9, service.getId()).getId());
			assertEquals(model.getId(), session.getMapper(AiModelConfigMapper.class)
					.findModel("tenant-large", 9, model.getId()).getId());
		}
	}

	private static void parseMapper(MybatisConfiguration configuration, String resource) throws Exception {
		InputStream inputStream = Resources.getResourceAsStream(resource);
		try {
			new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
		} finally {
			inputStream.close();
		}
	}

	private static String readRawApiKey(Long id) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement("select api_key from t_ai_model_service where id = ?")) {
			statement.setLong(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				resultSet.next();
				return resultSet.getString(1);
			}
		}
	}

	private static AiModelServiceConfig service(String tenantId, Integer appId, String apiKey) {
		AiModelServiceConfig service = new AiModelServiceConfig();
		service.setTenantId(tenantId);
		service.setAppId(appId);
		service.setServiceKey("openai");
		service.setProvider("openai");
		service.setLabel("OpenAI");
		service.setBaseUrl("https://api.openai.com/v1");
		service.setApiKey(apiKey);
		service.setAllowEmptyApiKey(false);
		service.setEnabled(true);
		service.setSortOrder(0);
		return service;
	}

	private static AiModelConfig model(AiModelServiceConfig service, String name) {
		AiModelConfig model = new AiModelConfig();
		model.setTenantId(service.getTenantId());
		model.setAppId(service.getAppId());
		model.setServiceId(service.getId());
		model.setName(name);
		model.setLabel(name);
		model.setCapabilities("chat");
		model.setEnabled(true);
		model.setSortOrder(0);
		return model;
	}
}
