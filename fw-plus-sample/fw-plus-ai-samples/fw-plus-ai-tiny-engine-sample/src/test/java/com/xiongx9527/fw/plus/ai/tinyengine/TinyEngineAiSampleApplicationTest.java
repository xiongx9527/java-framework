package com.xiongx9527.fw.plus.ai.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineAppMvcController;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginResult;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class TinyEngineAiSampleApplicationTest {

	@Test
	void startsWithTinyEnginePersistenceAndAiWebEndpoint() throws Exception {
		Class<?> applicationClass = Class.forName(
				"com.xiongx9527.fw.plus.ai.tinyengine.TinyEngineAiSampleApplication");
		try (ConfigurableApplicationContext context = new SpringApplication(applicationClass).run()) {
			assertThat(context.getBean(ResourceMapper.class)).isNotNull();
			assertThat(context.getBean(TinyEngineAiController.class)).isNotNull();
			int port = ((ServletWebServerApplicationContext) context).getWebServer().getPort();
			HttpURLConnection connection = (HttpURLConnection) new URL(
					"http://127.0.0.1:" + port + "/tiny-engine/index.html").openConnection();
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			try {
			assertAll(
					() -> assertThat(context.getBeansOfType(TinyEngineApiFacade.class)).hasSize(1),
					() -> assertThat(context.getBeansOfType(TinyEngineAppMvcController.class)).hasSize(1),
					() -> assertThat(connection.getResponseCode()).isEqualTo(200),
					() -> assertThat(connection.getContentType()).contains("text/html"));
			} finally {
				connection.disconnect();
			}
		}
	}

	@Test
	void platformInitializerRestoresOfficialDefaultPlatformWhenMissing() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_platform_initializer;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("create table t_platform (id int primary key, name varchar(255) not null, "
				+ "published boolean, description varchar(2000), latest_version varchar(255), latest_history_id int, "
				+ "material_history_id int, is_default boolean, tenant_id varchar(60), site_id varchar(60), "
				+ "created_by varchar(60) not null, last_updated_by varchar(60) not null, "
				+ "created_time timestamp not null, last_updated_time timestamp not null)");
		jdbcTemplate.execute("create table t_platform_history (id int primary key, ref_id int not null, "
				+ "version varchar(255) not null, name varchar(255) not null, publish_url varchar(255), "
				+ "description varchar(2000), material_history_id int not null, sub_count int, "
				+ "material_pkg_name varchar(255), material_version varchar(255), tenant_id varchar(60), "
				+ "site_id varchar(60), created_by varchar(60) not null, last_updated_by varchar(60) not null, "
				+ "created_time timestamp not null, last_updated_time timestamp not null)");

		new TinyEngineSampleAuthConfiguration().tinyEngineSampleDefaultPlatformInitializer(jdbcTemplate).run(null);

		assertAll(
				() -> assertThat(jdbcTemplate.queryForObject(
						"select count(1) from t_platform where id = 1 and name = 'default' and is_default = true",
						Integer.class)).isEqualTo(Integer.valueOf(1)),
				() -> assertThat(jdbcTemplate.queryForObject(
						"select count(1) from t_platform_history where id = 1 and ref_id = 1 and version = '1.0.0'",
						Integer.class)).isEqualTo(Integer.valueOf(1)));
	}

	@Test
	void sampleAuthUsesOfficialJwtStyleToken() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_jwt_auth;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("create table t_user (id int auto_increment primary key, username varchar(255), "
				+ "password varchar(255), salt varchar(255), email varchar(255), role varchar(255), "
				+ "enable boolean, is_admin boolean, is_public boolean)");
		jdbcTemplate.update("insert into t_user(username, password, salt, email, role, enable, is_admin, is_public) "
				+ "values (?, ?, ?, ?, ?, ?, ?, ?)", "alice", "pwd", null, "alice@example.com", "USER",
				Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
		TinyEngineSampleAuthConfiguration.SampleAuthProperties properties =
				new TinyEngineSampleAuthConfiguration.SampleAuthProperties();
		properties.setTokenTtlSeconds(3600L);
		properties.setSecret("tiny-engine-backend-secret-key-at-jwt-login");
		TinyEngineUserSystemService service = new TinyEngineSampleAuthConfiguration()
				.tinyEngineSampleUserSystemService(jdbcTemplate, properties);

		TinyEngineLoginResult login = service.login(new TinyEngineLoginRequest("alice", "pwd", "tenant-1"));
		TinyEnginePrincipal principal = service.currentUser(
				new TinyEngineCredential(login.getAccessToken(), "tenant-1"));

		assertAll(
				() -> assertThat(login.getTokenType()).isEqualTo("Bearer"),
				() -> assertThat(login.getExpiresIn()).isEqualTo(Long.valueOf(3600L)),
				() -> assertThat(login.getAccessToken().split("\\.")).hasSize(3),
				() -> assertThat(principal.getUserName()).isEqualTo("alice"),
				() -> assertThat(principal.getTenantId()).isEqualTo("tenant-1"),
				() -> assertThatThrownBy(() -> service.currentUser(new TinyEngineCredential(
						login.getAccessToken() + "x", "tenant-1")))
						.isInstanceOf(TinyEngineUnauthenticatedException.class)
						.hasMessageContaining("TinyEngine token is invalid or expired"));
	}

	@Test
	void sampleAuthDoesNotUseServerSideTokenStore() throws Exception {
		String source = new String(Files.readAllBytes(Paths.get(
				"src/main/java/com/xiongx9527/fw/plus/ai/tinyengine/"
						+ "TinyEngineSampleAuthConfiguration.java")), StandardCharsets.UTF_8);
		String pom = new String(Files.readAllBytes(Paths.get("pom.xml")), StandardCharsets.UTF_8);

		assertAll(
				() -> assertThat(source).contains("Jwts.builder()"),
				() -> assertThat(source).contains("parseSignedClaims"),
				() -> assertThat(source).contains("SECRET_STRING"),
				() -> assertThat(source).doesNotContain("ConcurrentHashMap"),
				() -> assertThat(source).doesNotContain("sessions.put"),
				() -> assertThat(source).doesNotContain("RedisTinyEngineUserSystemService"),
				() -> assertThat(source).doesNotContain("StringRedisTemplate"),
				() -> assertThat(pom).contains("<artifactId>jjwt-api</artifactId>"),
				() -> assertThat(pom).contains("<artifactId>jjwt-impl</artifactId>"),
				() -> assertThat(pom).contains("<artifactId>jjwt-jackson</artifactId>"),
				() -> assertThat(pom).doesNotContain("<artifactId>spring-boot-starter-data-redis</artifactId>"),
				() -> assertThat(pom).doesNotContain("<artifactId>sa-token-spring-boot-starter</artifactId>"));
	}
}
