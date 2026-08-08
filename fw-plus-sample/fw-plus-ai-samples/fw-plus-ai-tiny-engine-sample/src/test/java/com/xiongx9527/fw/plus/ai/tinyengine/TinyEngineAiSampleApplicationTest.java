package com.xiongx9527.fw.plus.ai.tinyengine;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiController;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api.TinyEngineApiFacade;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc.TinyEngineAppMvcController;
import com.xiongx9527.fw.plus.tinyengine.mapper.ResourceMapper;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginResult;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePasswordResetRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineRegistrationRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenant;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenantCreateRequest;
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
				+ "password varchar(255), salt varchar(255), public_key varchar(255), email varchar(255), tenant_id varchar(60), "
				+ "enable boolean, is_admin boolean, is_public boolean)");
		jdbcTemplate.update("insert into t_user(username, password, salt, email, tenant_id, enable, is_admin, is_public) "
				+ "values (?, ?, ?, ?, ?, ?, ?, ?)", "alice", "pwd", null, "alice@example.com", "tenant-1",
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
	void sampleRegistrationReturnsRecoveryCodeAndUsesItForPasswordReset() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_registration_recovery;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("create table t_user (id int auto_increment primary key, username varchar(255), "
				+ "password varchar(255), salt varchar(255), public_key varchar(255), email varchar(255), tenant_id varchar(60), "
				+ "enable boolean, is_admin boolean, is_public boolean)");
		TinyEngineSampleAuthConfiguration.SampleAuthProperties properties =
				new TinyEngineSampleAuthConfiguration.SampleAuthProperties();
		properties.setSecret("tiny-engine-backend-secret-key-at-jwt-login");
		TinyEngineUserSystemService service = new TinyEngineSampleAuthConfiguration()
				.tinyEngineSampleUserSystemService(jdbcTemplate, properties);

		TinyEnginePrincipal registered = service.register(
				new TinyEngineRegistrationRequest("bob", null, "OldPass123!", "system"));
		String publicKey = (String) registered.getClass().getMethod("getPublicKey").invoke(registered);
		service.requestPasswordReset(new TinyEnginePasswordResetRequest("bob", "system", publicKey,
				"NewPass123!"));
		TinyEngineLoginResult login = service.login(new TinyEngineLoginRequest("bob", "NewPass123!", "system"));

		assertAll(
				() -> assertThat(publicKey).isNotBlank(),
				() -> assertThat((String) registered.getClass().getMethod("getRecoveryCode").invoke(registered))
						.isEqualTo(publicKey),
				() -> assertThat(jdbcTemplate.queryForObject(
						"select public_key from t_user where username = 'bob'", String.class)).isEqualTo(publicKey),
				() -> assertThat(login.getAccessToken()).isNotBlank(),
				() -> assertThatThrownBy(() -> service.requestPasswordReset(
						new TinyEnginePasswordResetRequest("bob", "system", "wrong", "AnotherPass123!")))
						.isInstanceOf(TinyEngineUnauthenticatedException.class)
						.hasMessageContaining("TinyEngine recovery code is invalid"));
	}

	@Test
	void sampleAuthAssignsRegisteredUsersToPublicTenantAndRestrictsTenantCreateToAdmin() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_tenant_roles;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		createAuthTables(jdbcTemplate);
		TinyEngineSampleAuthConfiguration.SampleAuthProperties properties =
				new TinyEngineSampleAuthConfiguration.SampleAuthProperties();
		properties.setSecret("tiny-engine-backend-secret-key-at-jwt-login");
		TinyEngineSampleAuthConfiguration configuration = new TinyEngineSampleAuthConfiguration();
		TinyEngineUserSystemService service = configuration.tinyEngineSampleUserSystemService(jdbcTemplate, properties);
		properties.getAdmin().setUsername("admin");
		properties.getAdmin().setPassword("TinyEngine@123456");
		configuration.tinyEngineSampleAdminInitializer(jdbcTemplate, properties).run(null);

		TinyEnginePrincipal registered = service.register(
				new TinyEngineRegistrationRequest("charlie", null, "DevPass123!", null));
		TinyEngineLoginResult login = service.login(new TinyEngineLoginRequest("charlie", "DevPass123!", null));
		TinyEnginePrincipal current = service.currentUser(new TinyEngineCredential(login.getAccessToken(), null));
		TinyEngineLoginResult adminLogin = service.login(new TinyEngineLoginRequest("admin", "TinyEngine@123456", null));
		TinyEnginePrincipal admin = service.currentUser(new TinyEngineCredential(adminLogin.getAccessToken(), null));
		TinyEngineTenant tenant = service.createTenant(admin, new TinyEngineTenantCreateRequest(null, "team-a"));

		assertAll(
				() -> assertThat(registered.getTenantId()).isEqualTo("1"),
				() -> assertThat(current.getTenantId()).isEqualTo("1"),
				() -> assertThat(service.tenants(current)).extracting(TinyEngineTenant::getTenantId).contains("1"),
				() -> assertThat(jdbcTemplate.queryForObject("select tenant_id from t_user where username = 'charlie'",
						String.class)).isEqualTo("1"),
				() -> assertThat(jdbcTemplate.queryForObject("select is_admin from t_user where username = 'charlie'",
						Boolean.class)).isFalse(),
				() -> assertThat(jdbcTemplate.queryForObject("select count(1) from r_auth_users_units_roles "
						+ "where user_id = 2 and tenant_id = 1 and unit_id = 1 and unit_type = 'tenant' and role_id = 5",
						Integer.class)).isEqualTo(Integer.valueOf(1)),
				() -> assertThat(tenant.getTenantName()).isEqualTo("team-a"),
				() -> assertThat(jdbcTemplate.queryForObject("select enable from t_tenant where id = ?",
						Boolean.class, Integer.valueOf(tenant.getTenantId()))).isTrue(),
				() -> assertThat(service.tenants(admin)).extracting(TinyEngineTenant::getTenantName).contains("team-a"),
				() -> assertThatThrownBy(() -> service.createTenant(current,
						new TinyEngineTenantCreateRequest(null, "forbidden")))
						.isInstanceOf(TinyEngineUnauthenticatedException.class)
						.hasMessageContaining("Tinybuilder_Admin"));
	}

	@Test
	void sampleAuthRejectsDisabledTenantForLoginCurrentTokenAndSwitch() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_disabled_tenant;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		createAuthTables(jdbcTemplate);
		TinyEngineSampleAuthConfiguration.SampleAuthProperties properties =
				new TinyEngineSampleAuthConfiguration.SampleAuthProperties();
		properties.setSecret("tiny-engine-backend-secret-key-at-jwt-login");
		properties.getAdmin().setUsername("admin");
		properties.getAdmin().setPassword("TinyEngine@123456");
		TinyEngineSampleAuthConfiguration configuration = new TinyEngineSampleAuthConfiguration();
		TinyEngineUserSystemService service = configuration.tinyEngineSampleUserSystemService(jdbcTemplate, properties);
		configuration.tinyEngineSampleAdminInitializer(jdbcTemplate, properties).run(null);

		TinyEngineLoginResult login = service.login(new TinyEngineLoginRequest("admin", "TinyEngine@123456", null));
		TinyEnginePrincipal admin = service.currentUser(new TinyEngineCredential(login.getAccessToken(), null));
		TinyEngineTenant team = service.createTenant(admin, new TinyEngineTenantCreateRequest(null, "disabled-team"));
		jdbcTemplate.update("update t_tenant set enable = false where id = ?", Integer.valueOf(team.getTenantId()));

		assertAll(
				() -> assertThatThrownBy(() -> service.login(
						new TinyEngineLoginRequest("admin", "TinyEngine@123456", team.getTenantId())))
						.isInstanceOf(TinyEngineUnauthenticatedException.class)
						.hasMessageContaining("tenant is disabled"),
				() -> assertThatThrownBy(() -> service.currentUser(
						new TinyEngineCredential(login.getAccessToken(), team.getTenantId())))
						.isInstanceOf(TinyEngineUnauthenticatedException.class)
						.hasMessageContaining("tenant is disabled"),
				() -> assertThatThrownBy(() -> service.switchTenant(admin, team.getTenantId()))
						.isInstanceOf(TinyEngineUnauthenticatedException.class)
						.hasMessageContaining("tenant is disabled"),
				() -> assertThat(service.tenants(admin)).extracting(TinyEngineTenant::getTenantId)
						.doesNotContain(team.getTenantId()));
	}

	@Test
	void tenantEnableMigrationBackfillsOnlyLegacyNullValues() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_tenant_enable_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("create table t_tenant (id int primary key, enable boolean)");
		jdbcTemplate.update("insert into t_tenant(id, enable) values (1, null), (2, false)");
		String migration = new String(Files.readAllBytes(Paths.get(
				"../../../fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/"
						+ "fw-plus-tiny-engine-spring-boot-starter/src/main/resources/"
						+ "META-INF/tiny-engine/sql/h2/05_update_all_tables_ddl.sql")),
				StandardCharsets.UTF_8);
		for (String statement : migration.split(";")) {
			String sql = statement.trim();
			if (sql.startsWith("ALTER TABLE t_tenant") && sql.contains("enable")
					|| sql.startsWith("UPDATE t_tenant SET enable")) {
				jdbcTemplate.execute(sql);
			}
		}

		assertAll(
				() -> assertThat(jdbcTemplate.queryForObject("select enable from t_tenant where id = 1",
						Boolean.class)).isTrue(),
				() -> assertThat(jdbcTemplate.queryForObject("select enable from t_tenant where id = 2",
						Boolean.class)).isFalse(),
				() -> assertThatThrownBy(() -> jdbcTemplate.update(
						"insert into t_tenant(id, enable) values (3, null)"))
						.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class));
	}

	@Test
	void appEnableAndPermissionRoleCodeMigrationsPreserveLegacyDataAndConstraints() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource(
				"jdbc:h2:mem:tiny_engine_app_role_migration;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("create table t_app (id int primary key, enable boolean, code varchar(255))");
		jdbcTemplate.update("insert into t_app(id, enable, code) values (1, null, null), (2, false, null)");
		jdbcTemplate.execute("create table t_permission_role (id int primary key, name varchar(255) not null, "
				+ "role_type varchar(60) not null, code varchar(255))");
		jdbcTemplate.update("insert into t_permission_role(id, name, role_type, code) values "
				+ "(1, 'Tinybuilder_Admin', 'system', null), (2, 'Custom Role', 'app', null)");

		String migration = new String(Files.readAllBytes(Paths.get(
				"../../../fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/"
						+ "fw-plus-tiny-engine-spring-boot-starter/src/main/resources/"
						+ "META-INF/tiny-engine/sql/h2/05_update_all_tables_ddl.sql")),
				StandardCharsets.UTF_8);
		for (String statement : migration.split(";")) {
			String sql = statement.trim();
			boolean appStatement = (sql.startsWith("ALTER TABLE t_app")
					&& (sql.contains("enable") || sql.contains("code")))
					|| sql.startsWith("UPDATE t_app SET enable")
					|| sql.startsWith("UPDATE t_app SET code")
					|| (sql.startsWith("CREATE UNIQUE INDEX") && sql.contains("app_code"));
			boolean roleStatement = (sql.startsWith("ALTER TABLE t_permission_role") && sql.contains("code"))
					|| sql.startsWith("UPDATE t_permission_role SET code")
					|| (sql.startsWith("CREATE UNIQUE INDEX") && sql.contains("permission_role_code"));
			if (appStatement || roleStatement) {
				jdbcTemplate.execute(sql);
			}
		}

		assertAll(
				() -> assertThat(jdbcTemplate.queryForObject("select enable from t_app where id = 1",
						Boolean.class)).isTrue(),
				() -> assertThat(jdbcTemplate.queryForObject("select enable from t_app where id = 2",
						Boolean.class)).isFalse(),
				() -> assertThat(jdbcTemplate.queryForObject("select code from t_app where id = 1",
						String.class)).isEqualTo("app_1"),
				() -> assertThat(jdbcTemplate.queryForObject("select code from t_app where id = 2",
						String.class)).isEqualTo("app_2"),
				() -> assertThat(jdbcTemplate.queryForObject("select code from t_permission_role where id = 1",
						String.class)).isEqualTo("Tinybuilder_Admin"),
				() -> assertThat(jdbcTemplate.queryForObject("select code from t_permission_role where id = 2",
						String.class)).isEqualTo("role_2"),
				() -> assertThatThrownBy(() -> jdbcTemplate.update(
						"insert into t_app(id, enable, code) values (3, null, null)"))
						.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class),
				() -> assertThatThrownBy(() -> jdbcTemplate.update(
						"insert into t_app(id, enable, code) values (4, 1, 'app_1')"))
						.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class),
				() -> assertThatThrownBy(() -> jdbcTemplate.update(
						"insert into t_permission_role(id, name, role_type, code) "
								+ "values (3, 'Duplicate', 'app', 'Tinybuilder_Admin')"))
						.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class));
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

	private void createAuthTables(JdbcTemplate jdbcTemplate) {
		jdbcTemplate.execute("create table t_user (id int auto_increment primary key, username varchar(255), "
				+ "password varchar(255), salt varchar(255), public_key varchar(255), email varchar(255), tenant_id varchar(60), "
				+ "enable boolean, is_admin boolean, is_public boolean)");
		jdbcTemplate.execute("create table t_tenant (id int auto_increment primary key, name_cn varchar(255), "
				+ "name_en varchar(255), description varchar(2000), created_by varchar(60), last_updated_by varchar(60), "
				+ "created_time timestamp, last_updated_time timestamp, org_code varchar(255), is_system boolean, "
				+ "enable boolean not null default true)");
		jdbcTemplate.execute("create table t_permission_role (id int primary key, name varchar(255), "
				+ "role_type varchar(60), description varchar(2000), created_by varchar(60), created_time timestamp, "
				+ "last_updated_by varchar(60), last_updated_time timestamp)");
		jdbcTemplate.execute("create table r_auth_users_units_roles (id int auto_increment primary key, "
				+ "user_id int, unit_id int, unit_type varchar(60), tenant_id int, role_id int, "
				+ "expired_time timestamp, created_by varchar(60), created_time timestamp, "
				+ "last_updated_by varchar(60), last_updated_time timestamp, "
				+ "unique(user_id, unit_id, unit_type))");
	}
}
