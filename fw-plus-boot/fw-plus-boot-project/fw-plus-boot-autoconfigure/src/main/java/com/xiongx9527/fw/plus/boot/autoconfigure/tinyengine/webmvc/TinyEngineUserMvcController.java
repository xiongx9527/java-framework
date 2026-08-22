package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineUserRequestMapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineTenant;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/platform-center/api")
public class TinyEngineUserMvcController {

	private final TinyEngineUserSystemService userSystemService;
	private final TinyEngineCredentialExtractor credentialExtractor;

	public TinyEngineUserMvcController(TinyEngineUserSystemService userSystemService,
			TinyEngineCredentialExtractor credentialExtractor) {
		this.userSystemService = userSystemService;
		this.credentialExtractor = credentialExtractor;
	}

	@PostMapping("/user/login")
	public ResponseEntity<? extends Result<?>> login(@RequestBody Map<String, Object> body, HttpServletRequest request) {
		try {
			return TinyEngineWebResponses.ok(userSystemService.login(
					TinyEngineUserRequestMapper.toLoginRequest(body, request.getHeader("Host"))));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/user/register")
	public ResponseEntity<? extends Result<?>> register(@RequestBody Map<String, Object> body, HttpServletRequest request) {
		try {
			return TinyEngineWebResponses.ok(userSystemService.register(
					TinyEngineUserRequestMapper.toRegistrationRequest(body, request.getHeader("Host"))));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/user/forgot-password")
	public ResponseEntity<? extends Result<?>> forgotPassword(@RequestBody Map<String, Object> body) {
		try {
			userSystemService.requestPasswordReset(TinyEngineUserRequestMapper.toPasswordResetRequest(body));
			return TinyEngineWebResponses.ok();
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/user/logout")
	public ResponseEntity<? extends Result<?>> logout(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
		try {
			userSystemService.logout(credentialExtractor.extract(authorization, tenantId));
			return TinyEngineWebResponses.ok();
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/user/me")
	public ResponseEntity<? extends Result<?>> me(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
		try {
			return TinyEngineWebResponses.ok(currentUser(authorization, tenantId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/tenant/list")
	public ResponseEntity<? extends Result<?>> tenants(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
		try {
			TinyEnginePrincipal principal = currentUser(authorization, tenantId);
			List<TinyEngineTenant> tenants = userSystemService.tenants(principal);
			return TinyEngineWebResponses.ok(tenants);
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@GetMapping("/user/tenant")
	public ResponseEntity<? extends Result<?>> tenant(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String currentTenantId,
			@RequestParam String tenantId) {
		try {
			TinyEnginePrincipal principal = currentUser(authorization, currentTenantId);
			return TinyEngineWebResponses.ok(userSystemService.switchTenant(principal, tenantId));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/tenant/create")
	public ResponseEntity<? extends Result<?>> createTenant(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
			@RequestBody Map<String, Object> body) {
		try {
			TinyEnginePrincipal principal = currentUser(authorization, tenantId);
			return TinyEngineWebResponses.ok(userSystemService.createTenant(principal,
					TinyEngineUserRequestMapper.toTenantCreateRequest(body)));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	@PostMapping("/tenant/switch")
	public ResponseEntity<? extends Result<?>> switchTenant(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
			@RequestBody Map<String, Object> body) {
		try {
			TinyEnginePrincipal principal = currentUser(authorization, tenantId);
			return TinyEngineWebResponses.ok(userSystemService.switchTenant(principal,
					TinyEngineUserRequestMapper.string(body, "tenantId")));
		} catch (Throwable ex) {
			return TinyEngineWebResponses.error(ex);
		}
	}

	private TinyEnginePrincipal currentUser(String authorization, String tenantId) {
		TinyEngineCredential credential = credentialExtractor.extract(authorization, tenantId);
		return userSystemService.currentUser(credential);
	}
}
