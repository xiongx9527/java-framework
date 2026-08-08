package com.xiongx9527.fw.plus.ai.tinyengine;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineLoginResult;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePasswordResetRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineRegistrationRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenant;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenantCreateRequest;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Tenant;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(TinyEngineSampleAuthConfiguration.SampleAuthProperties.class)
public class TinyEngineSampleAuthConfiguration {

	private static final String DEFAULT_SECRET = "tiny-engine-backend-secret-key-at-jwt-login";
	private static final String DEFAULT_TENANT_ID = "1";
	private static final String DEFAULT_TENANT_NAME = "public";
	private static final String ROLE_ADMIN = "Tinybuilder_Admin";
	private static final String ROLE_TENANT_ADMIN = "Tinybuilder_Tenant_Admin";
	private static final String ROLE_APP_DEVELOPER = "Tinybuilder_App_Developer";

	@Bean
	public TinyEngineUserSystemService tinyEngineSampleUserSystemService(JdbcTemplate jdbcTemplate,
			SampleAuthProperties properties) {
		return new JdbcTinyEngineUserSystemService(jdbcTemplate, properties);
	}

	@Bean
	public ApplicationRunner tinyEngineSampleAdminInitializer(JdbcTemplate jdbcTemplate,
			SampleAuthProperties properties) {
		return args -> {
			if (!properties.getAdmin().isEnabled() || isBlank(properties.getAdmin().getUsername())
					|| isBlank(properties.getAdmin().getPassword())) {
				return;
			}
			Integer count = jdbcTemplate.queryForObject("select count(1) from t_user where username = ?",
					Integer.class, properties.getAdmin().getUsername());
			ensurePermissionRoles(jdbcTemplate);
			ensureDefaultTenant(jdbcTemplate);
			if (count != null && count.intValue() > 0) {
				jdbcTemplate.update("update t_user set tenant_id = ?, is_admin = ? where username = ?",
						DEFAULT_TENANT_ID, Boolean.TRUE, properties.getAdmin().getUsername());
			} else {
				String salt = randomSalt();
				jdbcTemplate.update("insert into t_user(username, password, salt, email, tenant_id, enable, is_admin, is_public) "
								+ "values (?, ?, ?, ?, ?, ?, ?, ?)",
						properties.getAdmin().getUsername(), hash(properties.getAdmin().getPassword(), salt), salt,
						properties.getAdmin().getEmail(), DEFAULT_TENANT_ID, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
			}
			Map<String, Object> admin = queryForMap(jdbcTemplate, "select * from t_user where username = ?",
					properties.getAdmin().getUsername());
			if (admin != null) {
				ensureTenantMembership(jdbcTemplate, string(admin, "id"), DEFAULT_TENANT_ID, Integer.valueOf(1),
						string(admin, "id"));
			}
		};
	}

	@Bean
	public ApplicationRunner tinyEngineSampleDefaultPlatformInitializer(JdbcTemplate jdbcTemplate) {
		return args -> {
			if (count(jdbcTemplate, "select count(1) from t_platform") == null) {
				return;
			}
			if (Integer.valueOf(0).equals(count(jdbcTemplate, "select count(1) from t_platform_history where id = 1"))) {
				jdbcTemplate.update("insert into t_platform_history(id, ref_id, version, name, publish_url, "
								+ "description, material_history_id, sub_count, material_pkg_name, material_version, "
								+ "tenant_id, site_id, created_by, last_updated_by, created_time, last_updated_time) "
								+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
						Integer.valueOf(1), Integer.valueOf(1), "1.0.0", "default", "http://tinyengine.com",
						"默认设计器", Integer.valueOf(1), Integer.valueOf(1),
						"@opentiny/lowcode-alpha-material-materialstwo-1505", "1.0.8", "1", "1", "1", "1");
			}
			if (Integer.valueOf(0).equals(count(jdbcTemplate, "select count(1) from t_platform"))) {
				jdbcTemplate.update("insert into t_platform(id, name, published, description, latest_version, "
								+ "latest_history_id, material_history_id, is_default, tenant_id, site_id, "
								+ "created_by, last_updated_by, created_time, last_updated_time) "
								+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
						Integer.valueOf(1), "default", Boolean.TRUE, "专用设计器", "1.0.0", Integer.valueOf(1),
						Integer.valueOf(1), Boolean.TRUE, "1", "1", "1", "1");
			}
		};
	}

	@ConfigurationProperties(prefix = "sample.tiny-engine.auth")
	public static class SampleAuthProperties {
		private String secret = defaultSecret();
		private long tokenTtlSeconds = 86400L;
		private Admin admin = new Admin();

		public String getSecret() {
			return secret;
		}

		public void setSecret(String secret) {
			this.secret = isBlank(secret) ? defaultSecret() : secret;
		}

		public long getTokenTtlSeconds() {
			return tokenTtlSeconds;
		}

		public void setTokenTtlSeconds(long tokenTtlSeconds) {
			this.tokenTtlSeconds = tokenTtlSeconds;
		}

		public Admin getAdmin() {
			return admin;
		}

		public void setAdmin(Admin admin) {
			this.admin = admin == null ? new Admin() : admin;
		}

		public static class Admin {
			private boolean enabled = true;
			private String username;
			private String password;
			private String email;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public String getUsername() {
				return username;
			}

			public void setUsername(String username) {
				this.username = username;
			}

			public String getPassword() {
				return password;
			}

			public void setPassword(String password) {
				this.password = password;
			}

			public String getEmail() {
				return email;
			}

			public void setEmail(String email) {
				this.email = email;
			}
		}
	}

	private static class JdbcTinyEngineUserSystemService implements TinyEngineUserSystemService {
		protected final JdbcTemplate jdbcTemplate;
		protected final SampleAuthProperties properties;

		private JdbcTinyEngineUserSystemService(JdbcTemplate jdbcTemplate, SampleAuthProperties properties) {
			this.jdbcTemplate = jdbcTemplate;
			this.properties = properties;
		}

		@Override
		public TinyEngineLoginResult login(TinyEngineLoginRequest request) {
			TinyEnginePrincipal principal = authenticatedPrincipal(request);
			long ttl = tokenTtl();
			return new TinyEngineLoginResult(generateToken(principal, ttl), "Bearer", Long.valueOf(ttl));
		}

		@Override
		public TinyEnginePrincipal currentUser(TinyEngineCredential credential) {
			if (credential == null || isBlank(credential.getAccessToken())) {
				throw new TinyEngineUnauthenticatedException("Missing Bearer token");
			}
			TinyEnginePrincipal principal = parsePrincipal(credential.getAccessToken());
			if (!isBlank(credential.getTenantId()) && !credential.getTenantId().equals(principal.getTenantId())) {
				principal = tenantPrincipal(principal, credential.getTenantId());
			}
			requireTenantEnabled(principal.getTenantId());
			return hydratePrincipal(principal);
		}

		@Override
		public void logout(TinyEngineCredential credential) {
		}

		@Override
		public TinyEnginePrincipal register(TinyEngineRegistrationRequest request) {
			if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
				throw new TinyEngineUnsupportedCapabilityException("TinyEngine username and password are required");
			}
			if (userByUsername(request.getUsername()) != null) {
				throw new TinyEngineUnsupportedCapabilityException("TinyEngine username already exists");
			}
			String salt = randomSalt();
			String publicKey = randomRecoveryCode();
			String tenantId = normalizeTenantId(request.getTenantId());
			jdbcTemplate.update("insert into t_user(username, password, salt, public_key, email, tenant_id, enable, is_admin, is_public) "
							+ "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
					request.getUsername(), hash(request.getPassword(), salt), salt, publicKey, null, tenantId,
					Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
			Map<String, Object> user = userByUsername(request.getUsername());
			if (DEFAULT_TENANT_ID.equals(tenantId)) {
				ensureDefaultTenant(jdbcTemplate);
				ensureTenantMembership(jdbcTemplate, string(user, "id"), DEFAULT_TENANT_ID, Integer.valueOf(5),
						string(user, "id"));
			}
			return new TinyEngineRegistrationPrincipal(principal(user, tenantId), publicKey);
		}

		@Override
		public void requestPasswordReset(TinyEnginePasswordResetRequest request) {
			if (request == null || isBlank(request.getUsername()) || isBlank(request.getNewPassword())) {
				throw new TinyEngineUnsupportedCapabilityException("TinyEngine username and password are required");
			}
			Map<String, Object> user = userByUsername(request.getUsername());
			if (user == null || !enabled(user)) {
				throw new TinyEngineUnauthenticatedException("TinyEngine username is invalid");
			}
			String publicKey = string(user, "public_key");
			if (isBlank(publicKey) || isBlank(request.getVerificationCode())
					|| !publicKey.equals(request.getVerificationCode())) {
				throw new TinyEngineUnauthenticatedException("TinyEngine recovery code is invalid");
			}
			String salt = randomSalt();
			if (jdbcTemplate.update("update t_user set password = ?, salt = ? where username = ?",
					hash(request.getNewPassword(), salt), salt, request.getUsername()) < 1) {
				throw new TinyEngineUnauthenticatedException("TinyEngine username is invalid");
			}
		}

		@Override
		public List<TinyEngineTenant> tenants(TinyEnginePrincipal principal) {
			TinyEnginePrincipal current = requirePrincipal(principal);
			List<Tenant> tenants = queryTenants(current.getUserId(), current.getTenantId());
			if (tenants.isEmpty()) {
				return Collections.singletonList(new TinyEngineTenant(current.getTenantId(), current.getTenantId()));
			}
			List<TinyEngineTenant> result = new ArrayList<TinyEngineTenant>();
			for (Tenant tenant : tenants) {
				result.add(new TinyEngineTenant(tenant.getId(), tenantName(tenant)));
			}
			return result;
		}

		@Override
		public TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId) {
			TinyEnginePrincipal current = requirePrincipal(principal);
			String targetTenantId = isBlank(tenantId) ? current.getTenantId() : tenantId;
			targetTenantId = normalizeTenantId(targetTenantId);
			requireTenantEnabled(targetTenantId);
			if (!hasTenant(current.getUserId(), targetTenantId)) {
				throw new TinyEngineUnauthenticatedException("TinyEngine tenant is not assigned to current user");
			}
			return hydratePrincipal(new TinyEnginePrincipal(current.getUserId(), current.getUserName(), targetTenantId,
					targetTenantId, current.getPlatformId()));
		}

		@Override
		public TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request) {
			TinyEnginePrincipal current = requirePrincipal(principal);
			if (!isAdmin(current.getUserId())) {
				throw new TinyEngineUnauthenticatedException("TinyEngine tenant create requires Tinybuilder_Admin");
			}
			String tenantName = request == null || isBlank(request.getTenantName()) ? current.getTenantId()
					: request.getTenantName();
			String tenantId = request == null || isBlank(request.getTenantId()) ? null : normalizeTenantId(request.getTenantId());
			Tenant tenant = saveTenant(tenantId, tenantName, current.getUserId());
			ensureTenantMembership(jdbcTemplate, current.getUserId(), tenant.getId(), Integer.valueOf(2), current.getUserId());
			return new TinyEngineTenant(tenant.getId(), tenantName(tenant));
		}

		@Override
		public void check(TinyEnginePrincipal principal, String permission) {
			requirePrincipal(principal);
		}

		private TinyEnginePrincipal requirePrincipal(TinyEnginePrincipal principal) {
			if (principal == null) {
				throw new TinyEngineUnauthenticatedException("TinyEngine principal is required");
			}
			requireTenantEnabled(principal.getTenantId());
			return principal;
		}

		protected TinyEnginePrincipal authenticatedPrincipal(TinyEngineLoginRequest request) {
			if (request == null || isBlank(request.getUsername()) || isBlank(request.getPassword())) {
				throw new TinyEngineUnauthenticatedException("TinyEngine username and password are required");
			}
			Map<String, Object> user = userByUsername(request.getUsername());
			if (user == null || !enabled(user) || !matches(request.getPassword(), string(user, "password"),
					string(user, "salt"))) {
				throw new TinyEngineUnauthenticatedException("TinyEngine username or password is invalid");
			}
			String tenantId = normalizeTenantId(isBlank(request.getTenantId()) ? string(user, "tenant_id")
					: request.getTenantId());
			requireTenantEnabled(tenantId);
			return principal(user, tenantId);
		}

		protected long tokenTtl() {
			return properties.getTokenTtlSeconds() <= 0 ? 86400L : properties.getTokenTtlSeconds();
		}

		protected String generateToken(TinyEnginePrincipal principal, long ttl) {
			Map<String, Object> claims = new HashMap<String, Object>();
			claims.put("username", principal.getUserName());
			claims.put("roles", userLevelRoleOf(principal.getUserId()));
			claims.put("userId", principal.getUserId());
			claims.put("platformId", principal.getPlatformId());
			claims.put("tenantId", principal.getTenantId());
			claims.put("renterId", principal.getRenterId());
			claims.put("tenants", tenantsClaim(principal));
			return Jwts.builder()
					.claims(claims)
					.subject(principal.getUserName())
					.issuedAt(new Date())
					.expiration(new Date(System.currentTimeMillis() + ttl * 1000L))
					.signWith(secretKey())
					.compact();
		}

		protected TinyEnginePrincipal parsePrincipal(String token) {
			try {
				Claims claims = Jwts.parser()
						.verifyWith(secretKey())
						.build()
						.parseSignedClaims(token)
						.getPayload();
				if (claims.getExpiration() == null || claims.getExpiration().before(new Date())) {
					throw new TinyEngineUnauthenticatedException("TinyEngine token is invalid or expired");
				}
				String username = string(claims, "username");
				if (isBlank(username)) {
					username = claims.getSubject();
				}
				String userId = string(claims, "userId");
				if (isBlank(username) || isBlank(userId)) {
					throw new TinyEngineUnauthenticatedException("TinyEngine token is invalid or expired");
				}
				String tenantId = normalizeTenantId(string(claims, "tenantId"));
				String renterId = normalizeTenantId(string(claims, "renterId"));
				Integer platformId = integer(claims.get("platformId"));
				return new TinyEnginePrincipal(userId, username, tenantId, renterId,
						platformId == null ? Integer.valueOf(0) : platformId);
			} catch (TinyEngineUnauthenticatedException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new TinyEngineUnauthenticatedException("TinyEngine token is invalid or expired");
			}
		}

		private List<Map<String, Object>> tenantsClaim(TinyEnginePrincipal principal) {
			List<Map<String, Object>> tenants = new ArrayList<Map<String, Object>>();
			for (Tenant tenantItem : queryTenants(principal.getUserId(), principal.getTenantId())) {
				Map<String, Object> tenant = new HashMap<String, Object>();
				tenant.put("id", tenantItem.getId());
				tenant.put("orgCode", tenantItem.getOrgCode());
				tenant.put("nameCn", tenantItem.getNameCn());
				tenant.put("nameEn", tenantItem.getNameEn());
				tenant.put("isInUse", Boolean.TRUE.equals(tenantItem.getIsInUse()));
				tenants.add(tenant);
			}
			if (tenants.isEmpty()) {
				Map<String, Object> tenant = new HashMap<String, Object>();
				tenant.put("id", principal.getTenantId());
				tenant.put("orgCode", principal.getTenantId());
				tenant.put("nameCn", principal.getTenantId());
				tenant.put("nameEn", principal.getTenantId());
				tenant.put("isInUse", Boolean.TRUE);
				tenants.add(tenant);
			}
			return tenants;
		}

		private SecretKey secretKey() {
			return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
		}

		protected TinyEnginePrincipal tenantPrincipal(TinyEnginePrincipal principal, String tenantId) {
			String normalizedTenantId = normalizeTenantId(tenantId);
			return new TinyEnginePrincipal(principal.getUserId(), principal.getUserName(),
					normalizedTenantId, normalizedTenantId, principal.getPlatformId());
		}

		protected Map<String, Object> userByUsername(String username) {
			try {
				return jdbcTemplate.queryForMap("select * from t_user where username = ?", username);
			} catch (EmptyResultDataAccessException ignored) {
				return null;
			}
		}

		protected TinyEnginePrincipal principal(Map<String, Object> user, String tenantId) {
			String id = String.valueOf(user.get("id"));
			String username = string(user, "username");
			String normalizedTenantId = normalizeTenantId(tenantId);
			return new SampleTinyEnginePrincipal(id, username, normalizedTenantId, normalizedTenantId,
					Integer.valueOf(0), userLevelRole(user), queryTenants(id, normalizedTenantId));
		}

		private TinyEnginePrincipal hydratePrincipal(TinyEnginePrincipal principal) {
			Map<String, Object> user = queryForMap(jdbcTemplate, "select * from t_user where id = ?",
					principal.getUserId());
			String role = user == null ? userLevelRoleOf(principal.getUserId()) : userLevelRole(user);
			return new SampleTinyEnginePrincipal(principal.getUserId(), principal.getUserName(),
					principal.getTenantId(), principal.getRenterId(), principal.getPlatformId(), role,
					queryTenants(principal.getUserId(), principal.getTenantId()));
		}

		private List<Tenant> queryTenants(String userId, String currentTenantId) {
			if (!tableExists("r_auth_users_units_roles") || !tableExists("t_tenant")) {
				return Collections.emptyList();
			}
			return jdbcTemplate.query("select distinct tt.* from t_tenant tt "
							+ "inner join r_auth_users_units_roles raur on tt.id = raur.tenant_id "
							+ "where raur.user_id = ? and tt.enable = true order by tt.id",
					(rs, rowNum) -> {
						Tenant tenant = new Tenant();
						tenant.setId(String.valueOf(rs.getObject("id")));
						tenant.setNameCn(rs.getString("name_cn"));
						tenant.setNameEn(rs.getString("name_en"));
						tenant.setDescription(rs.getString("description"));
						tenant.setOrgCode(hasColumn(rs, "org_code") ? rs.getString("org_code") : tenant.getId());
						tenant.setIsSystem(hasColumn(rs, "is_system") && bool(rs.getObject("is_system")));
						tenant.setIsEnable(bool(rs.getObject("enable")));
						tenant.setIsInUse(tenant.getId().equals(normalizeTenantId(currentTenantId)));
						return tenant;
					}, Integer.valueOf(userId));
		}

		private boolean hasTenant(String userId, String tenantId) {
			if (!tableExists("r_auth_users_units_roles")) {
				return true;
			}
			Integer count = jdbcTemplate.queryForObject("select count(1) from r_auth_users_units_roles raur "
					+ "inner join t_tenant tt on tt.id = raur.tenant_id "
					+ "where raur.user_id = ? and raur.tenant_id = ? and tt.enable = true", Integer.class, Integer.valueOf(userId),
					Integer.valueOf(normalizeTenantId(tenantId)));
			return count != null && count.intValue() > 0;
		}

		private Tenant saveTenant(String tenantId, String tenantName, String operatorId) {
			if (!tableExists("t_tenant")) {
				throw new TinyEngineUnsupportedCapabilityException("TinyEngine tenant table is not initialized");
			}
			String normalizedName = isBlank(tenantName) ? DEFAULT_TENANT_NAME : tenantName;
			if (!isBlank(tenantId) && findTenant(tenantId) != null) {
				return findTenant(tenantId);
			}
			jdbcTemplate.update("insert into t_tenant(name_cn, name_en, description, created_by, last_updated_by, "
							+ "created_time, last_updated_time, org_code) values (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, "
							+ "CURRENT_TIMESTAMP, ?)", normalizedName, normalizedName, normalizedName, operatorId,
					operatorId, normalizedName);
			return findTenantByName(normalizedName);
		}

		private Tenant findTenant(String tenantId) {
			Map<String, Object> tenant = queryForMap(jdbcTemplate, "select * from t_tenant where id = ?",
					normalizeTenantId(tenantId));
			return tenant == null ? null : tenant(tenant, false);
		}

		private Tenant findTenantByName(String tenantName) {
			Map<String, Object> tenant = queryForMap(jdbcTemplate,
					"select * from t_tenant where name_en = ? order by id desc limit 1", tenantName);
			return tenant == null ? null : tenant(tenant, false);
		}

		private Tenant tenant(Map<String, Object> row, boolean inUse) {
			Tenant tenant = new Tenant();
			tenant.setId(string(row, "id"));
			tenant.setNameCn(string(row, "name_cn"));
			tenant.setNameEn(string(row, "name_en"));
			tenant.setDescription(string(row, "description"));
			tenant.setOrgCode(string(row, "org_code"));
			tenant.setIsSystem(bool(row.get("is_system")));
			tenant.setIsEnable(bool(row.get("enable")));
			tenant.setIsInUse(Boolean.valueOf(inUse));
			return tenant;
		}

		private void requireTenantEnabled(String tenantId) {
			if (!tableExists("t_tenant")) {
				return;
			}
			Map<String, Object> tenant = queryForMap(jdbcTemplate,
					"select enable from t_tenant where id = ?", normalizeTenantId(tenantId));
			if (tenant == null || !bool(tenant.get("enable"))) {
				throw new TinyEngineUnauthenticatedException("TinyEngine tenant is disabled");
			}
		}

		private boolean isAdmin(String userId) {
			Map<String, Object> user = queryForMap(jdbcTemplate, "select * from t_user where id = ?", userId);
			return user != null && bool(user.get("is_admin"));
		}

		private String userLevelRoleOf(String userId) {
			Map<String, Object> user = queryForMap(jdbcTemplate, "select * from t_user where id = ?", userId);
			return user == null ? ROLE_APP_DEVELOPER : userLevelRole(user);
		}

		private String userLevelRole(Map<String, Object> user) {
			return bool(user.get("is_admin")) ? ROLE_ADMIN : ROLE_APP_DEVELOPER;
		}

		private boolean tableExists(String tableName) {
			return count(jdbcTemplate, "select count(1) from " + tableName) != null;
		}

		protected boolean enabled(Map<String, Object> user) {
			Object value = user.get("enable");
			return value == null || bool(value);
		}

		private boolean bool(Object value) {
			return Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value))
					|| "true".equalsIgnoreCase(String.valueOf(value));
		}

		protected boolean matches(String rawPassword, String storedPassword, String salt) {
			if (isBlank(storedPassword)) {
				return false;
			}
			if (!isBlank(salt)) {
				return storedPassword.equals(hash(rawPassword, salt));
			}
			return storedPassword.equals(rawPassword);
		}

		protected String string(Map<String, Object> user, String key) {
			Object value = user.get(key);
			return value == null ? null : String.valueOf(value);
		}

		protected String string(Claims claims, String key) {
			Object value = claims.get(key);
			return value == null ? null : String.valueOf(value);
		}

		private Integer integer(Object value) {
			if (value instanceof Integer) {
				return (Integer) value;
			}
			if (value instanceof Number) {
				return Integer.valueOf(((Number) value).intValue());
			}
			if (value == null) {
				return null;
			}
			return Integer.valueOf(String.valueOf(value));
		}
	}

	private static String randomSalt() {
		byte[] bytes = new byte[16];
		new SecureRandom().nextBytes(bytes);
		return hex(bytes);
	}

	private static String randomRecoveryCode() {
		byte[] bytes = new byte[32];
		new SecureRandom().nextBytes(bytes);
		return hex(bytes);
	}

	private static String hash(String password, String salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return hex(digest.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8)));
		} catch (Exception ex) {
			throw new IllegalStateException("TinyEngine password hash failed", ex);
		}
	}

	private static String hex(byte[] bytes) {
		StringBuilder builder = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			String value = Integer.toHexString(b & 0xff);
			if (value.length() == 1) {
				builder.append('0');
			}
			builder.append(value);
		}
		return builder.toString();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static String normalizeTenantId(String tenantId) {
		if (isBlank(tenantId) || "system".equalsIgnoreCase(tenantId)) {
			return DEFAULT_TENANT_ID;
		}
		return tenantId;
	}

	private static void ensurePermissionRoles(JdbcTemplate jdbcTemplate) {
		if (count(jdbcTemplate, "select count(1) from t_permission_role") == null) {
			return;
		}
		ensurePermissionRole(jdbcTemplate, Integer.valueOf(1), ROLE_ADMIN, "system", "超级管理员");
		ensurePermissionRole(jdbcTemplate, Integer.valueOf(2), ROLE_TENANT_ADMIN, "tenant", "组织管理员");
		ensurePermissionRole(jdbcTemplate, Integer.valueOf(3), "Tinybuilder_Platform_Admin", "platform", "设计器管理员");
		ensurePermissionRole(jdbcTemplate, Integer.valueOf(4), "Tinybuilder_App_Admin", "app", "应用管理员");
		ensurePermissionRole(jdbcTemplate, Integer.valueOf(5), ROLE_APP_DEVELOPER, "app", "应用开发者");
		ensurePermissionRole(jdbcTemplate, Integer.valueOf(6), "Guest", "guest", "游客");
	}

	private static void ensurePermissionRole(JdbcTemplate jdbcTemplate, Integer id, String name, String roleType,
			String description) {
		Integer count = jdbcTemplate.queryForObject("select count(1) from t_permission_role where id = ? or name = ?",
				Integer.class, id, name);
		if (count != null && count.intValue() > 0) {
			if (hasPermissionRoleTypeColumn(jdbcTemplate)) {
				jdbcTemplate.update("update t_permission_role set role_type = ? where id = ? or name = ?",
						roleType, id, name);
			}
			return;
		}
		if (hasPermissionRoleTypeColumn(jdbcTemplate)) {
			jdbcTemplate.update("insert into t_permission_role(id, name, role_type, description, created_by, "
							+ "last_updated_by, created_time, last_updated_time) values (?, ?, ?, ?, ?, ?, "
							+ "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", id, name, roleType, description, "1", "1");
		} else {
			jdbcTemplate.update("insert into t_permission_role(id, name, description, created_by, last_updated_by, "
							+ "created_time, last_updated_time) values (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
					id, name, description, "1", "1");
		}
	}

	private static boolean hasPermissionRoleTypeColumn(JdbcTemplate jdbcTemplate) {
		try {
			jdbcTemplate.queryForObject("select role_type from t_permission_role where 1 = 0", Object.class);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static void ensureDefaultTenant(JdbcTemplate jdbcTemplate) {
		if (count(jdbcTemplate, "select count(1) from t_tenant") == null) {
			return;
		}
		Integer count = jdbcTemplate.queryForObject("select count(1) from t_tenant where id = ?",
				Integer.class, Integer.valueOf(DEFAULT_TENANT_ID));
		if (count != null && count.intValue() > 0) {
			return;
		}
		if (hasTenantSystemColumn(jdbcTemplate)) {
			jdbcTemplate.update("insert into t_tenant(id, name_cn, name_en, description, is_system, created_by, "
							+ "last_updated_by, created_time, last_updated_time, org_code) values (?, ?, ?, ?, ?, ?, ?, "
							+ "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)", Integer.valueOf(DEFAULT_TENANT_ID), "公共租户",
					DEFAULT_TENANT_NAME, "Default tenant for new user to explore.", Boolean.TRUE, "1", "1",
					DEFAULT_TENANT_NAME);
		} else {
			jdbcTemplate.update("insert into t_tenant(id, name_cn, name_en, description, created_by, last_updated_by, "
							+ "created_time, last_updated_time, org_code) values (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, "
							+ "CURRENT_TIMESTAMP, ?)", Integer.valueOf(DEFAULT_TENANT_ID), "公共租户", DEFAULT_TENANT_NAME,
					"Default tenant for new user to explore.", "1", "1", DEFAULT_TENANT_NAME);
		}
	}

	private static boolean hasTenantSystemColumn(JdbcTemplate jdbcTemplate) {
		try {
			jdbcTemplate.queryForObject("select is_system from t_tenant where 1 = 0", Object.class);
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}

	private static void ensureTenantMembership(JdbcTemplate jdbcTemplate, String userId, String tenantId, Integer roleId,
			String operatorId) {
		if (isBlank(userId) || count(jdbcTemplate, "select count(1) from r_auth_users_units_roles") == null) {
			return;
		}
		Integer count = jdbcTemplate.queryForObject("select count(1) from r_auth_users_units_roles "
						+ "where user_id = ? and unit_id = ? and unit_type = ?",
				Integer.class, Integer.valueOf(userId), Integer.valueOf(normalizeTenantId(tenantId)), "tenant");
		if (count != null && count.intValue() > 0) {
			jdbcTemplate.update("update r_auth_users_units_roles set tenant_id = ?, role_id = ?, last_updated_by = ?, "
							+ "last_updated_time = CURRENT_TIMESTAMP where user_id = ? and unit_id = ? and unit_type = ?",
					Integer.valueOf(normalizeTenantId(tenantId)), roleId, operatorId, Integer.valueOf(userId),
					Integer.valueOf(normalizeTenantId(tenantId)), "tenant");
			return;
		}
		jdbcTemplate.update("insert into r_auth_users_units_roles(user_id, unit_id, unit_type, tenant_id, role_id, "
						+ "created_by, created_time, last_updated_by, last_updated_time) values (?, ?, ?, ?, ?, ?, "
						+ "CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP)", Integer.valueOf(userId),
				Integer.valueOf(normalizeTenantId(tenantId)), "tenant", Integer.valueOf(normalizeTenantId(tenantId)),
				roleId, operatorId, operatorId);
	}

	private static String defaultSecret() {
		String secretString = System.getenv("SECRET_STRING");
		return isBlank(secretString) ? DEFAULT_SECRET : secretString;
	}

	private static class TinyEngineRegistrationPrincipal extends TinyEnginePrincipal {
		private final String publicKey;

		TinyEngineRegistrationPrincipal(TinyEnginePrincipal principal, String publicKey) {
			super(principal.getUserId(), principal.getUserName(), principal.getTenantId(),
					principal.getRenterId(), principal.getPlatformId());
			this.publicKey = publicKey;
		}

		public String getPublicKey() {
			return publicKey;
		}

		public String getRecoveryCode() {
			return publicKey;
		}
	}

	private static Integer count(JdbcTemplate jdbcTemplate, String sql) {
		try {
			return jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (DataAccessException ignored) {
			return null;
		}
	}

	private static Map<String, Object> queryForMap(JdbcTemplate jdbcTemplate, String sql, Object... args) {
		try {
			return jdbcTemplate.queryForMap(sql, args);
		} catch (EmptyResultDataAccessException ignored) {
			return null;
		} catch (DataAccessException ignored) {
			return null;
		}
	}

	private static String string(Map<String, Object> row, String key) {
		if (row == null) {
			return null;
		}
		Object value = row.get(key);
		return value == null ? null : String.valueOf(value);
	}

	private static boolean hasColumn(java.sql.ResultSet rs, String columnName) {
		try {
			java.sql.ResultSetMetaData metaData = rs.getMetaData();
			for (int i = 1; i <= metaData.getColumnCount(); i++) {
				if (columnName.equalsIgnoreCase(metaData.getColumnName(i))) {
					return true;
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	private static String tenantName(Tenant tenant) {
		if (tenant == null) {
			return DEFAULT_TENANT_NAME;
		}
		if (!isBlank(tenant.getNameEn())) {
			return tenant.getNameEn();
		}
		if (!isBlank(tenant.getNameCn())) {
			return tenant.getNameCn();
		}
		return tenant.getId();
	}

	private static class SampleTinyEnginePrincipal extends TinyEnginePrincipal {
		private final String id;
		private final String username;
		private final String role;
		private final String roles;
		private final List<Tenant> tenant;
		private final List<Tenant> tenants;

		SampleTinyEnginePrincipal(String userId, String userName, String tenantId, String renterId, Integer platformId,
				String role, List<Tenant> tenants) {
			super(userId, userName, tenantId, renterId, platformId);
			this.id = userId;
			this.username = userName;
			this.role = role;
			this.roles = role;
			this.tenant = tenants == null ? Collections.<Tenant>emptyList() : tenants;
			this.tenants = this.tenant;
		}

		public String getId() {
			return id;
		}

		public String getUsername() {
			return username;
		}

		public String getRole() {
			return role;
		}

		public String getRoles() {
			return roles;
		}

		public List<Tenant> getTenant() {
			return tenant;
		}

		public List<Tenant> getTenants() {
			return tenants;
		}
	}
}
