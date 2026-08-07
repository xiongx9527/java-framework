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
			if (count != null && count.intValue() > 0) {
				return;
			}
			String salt = randomSalt();
			jdbcTemplate.update("insert into t_user(username, password, salt, email, role, enable, is_admin, is_public) "
							+ "values (?, ?, ?, ?, ?, ?, ?, ?)",
					properties.getAdmin().getUsername(), hash(properties.getAdmin().getPassword(), salt), salt,
					properties.getAdmin().getEmail(), "admin", Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
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
				return tenantPrincipal(principal, credential.getTenantId());
			}
			return principal;
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
			jdbcTemplate.update("insert into t_user(username, password, salt, email, role, enable, is_admin, is_public) "
							+ "values (?, ?, ?, ?, ?, ?, ?, ?)",
					request.getUsername(), hash(request.getPassword(), salt), salt, null, "user",
					Boolean.TRUE, Boolean.FALSE, Boolean.FALSE);
			Map<String, Object> user = userByUsername(request.getUsername());
			String tenantId = isBlank(request.getTenantId()) ? "system" : request.getTenantId();
			return principal(user, tenantId);
		}

		@Override
		public void requestPasswordReset(TinyEnginePasswordResetRequest request) {
			throw new TinyEngineUnsupportedCapabilityException("TinyEngine password reset is not configured");
		}

		@Override
		public List<TinyEngineTenant> tenants(TinyEnginePrincipal principal) {
			TinyEnginePrincipal current = requirePrincipal(principal);
			return Collections.singletonList(new TinyEngineTenant(current.getTenantId(), current.getTenantId()));
		}

		@Override
		public TinyEnginePrincipal switchTenant(TinyEnginePrincipal principal, String tenantId) {
			TinyEnginePrincipal current = requirePrincipal(principal);
			String targetTenantId = isBlank(tenantId) ? current.getTenantId() : tenantId;
			return new TinyEnginePrincipal(current.getUserId(), current.getUserName(), targetTenantId,
					targetTenantId, current.getPlatformId());
		}

		@Override
		public TinyEngineTenant createTenant(TinyEnginePrincipal principal, TinyEngineTenantCreateRequest request) {
			TinyEnginePrincipal current = requirePrincipal(principal);
			String tenantName = request == null || isBlank(request.getTenantName()) ? current.getTenantId()
					: request.getTenantName();
			String tenantId = request == null || isBlank(request.getTenantId()) ? tenantName : request.getTenantId();
			return new TinyEngineTenant(tenantId, tenantName);
		}

		@Override
		public void check(TinyEnginePrincipal principal, String permission) {
			requirePrincipal(principal);
		}

		private TinyEnginePrincipal requirePrincipal(TinyEnginePrincipal principal) {
			if (principal == null) {
				throw new TinyEngineUnauthenticatedException("TinyEngine principal is required");
			}
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
			String tenantId = isBlank(request.getTenantId()) ? "system" : request.getTenantId();
			return principal(user, tenantId);
		}

		protected long tokenTtl() {
			return properties.getTokenTtlSeconds() <= 0 ? 86400L : properties.getTokenTtlSeconds();
		}

		protected String generateToken(TinyEnginePrincipal principal, long ttl) {
			Map<String, Object> claims = new HashMap<String, Object>();
			claims.put("username", principal.getUserName());
			claims.put("roles", "USER");
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
			Map<String, Object> tenant = new HashMap<String, Object>();
			tenant.put("id", principal.getTenantId());
			tenant.put("orgCode", principal.getTenantId());
			tenant.put("nameCn", principal.getTenantId());
			tenant.put("nameEn", principal.getTenantId());
			tenant.put("isInUse", Boolean.TRUE);
			List<Map<String, Object>> tenants = new ArrayList<Map<String, Object>>();
			tenants.add(tenant);
			return tenants;
		}

		private SecretKey secretKey() {
			return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
		}

		protected TinyEnginePrincipal tenantPrincipal(TinyEnginePrincipal principal, String tenantId) {
			return new TinyEnginePrincipal(principal.getUserId(), principal.getUserName(),
					tenantId, tenantId, principal.getPlatformId());
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
			return new TinyEnginePrincipal(id, username, normalizedTenantId, normalizedTenantId, Integer.valueOf(0));
		}

		protected boolean enabled(Map<String, Object> user) {
			Object value = user.get("enable");
			return value == null || Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value))
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
		return isBlank(tenantId) ? "system" : tenantId;
	}

	private static String defaultSecret() {
		String secretString = System.getenv("SECRET_STRING");
		return isBlank(secretString) ? DEFAULT_SECRET : secretString;
	}

	private static Integer count(JdbcTemplate jdbcTemplate, String sql) {
		try {
			return jdbcTemplate.queryForObject(sql, Integer.class);
		} catch (DataAccessException ignored) {
			return null;
		}
	}
}
