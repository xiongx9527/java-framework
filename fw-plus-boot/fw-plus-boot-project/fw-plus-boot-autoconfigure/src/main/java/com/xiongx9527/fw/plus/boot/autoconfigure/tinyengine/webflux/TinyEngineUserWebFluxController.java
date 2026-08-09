package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineUserRequestMapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.Callable;

@RestController
@RequestMapping("${fw.plus.tiny-engine.web.base-path:}/platform-center/api")
public class TinyEngineUserWebFluxController {

	private final TinyEngineUserSystemService userSystemService;
	private final TinyEngineCredentialExtractor credentialExtractor;
	private final TinyEngineReactiveExecutor executor;
	private final TinyEngineRequestUserScope userScope;

	public TinyEngineUserWebFluxController(TinyEngineUserSystemService userSystemService,
			TinyEngineCredentialExtractor credentialExtractor, TinyEngineReactiveExecutor executor,
			TinyEngineRequestUserScope userScope) {
		this.userSystemService = userSystemService;
		this.credentialExtractor = credentialExtractor;
		this.executor = executor;
		this.userScope = userScope;
	}

	@PostMapping("/user/login")
	public Mono<ResponseEntity<? extends Result<?>>> login(@RequestBody Map<String, Object> body,
			ServerWebExchange exchange) {
		return submit(() -> TinyEngineWebResponses.ok(userSystemService.login(
				TinyEngineUserRequestMapper.toLoginRequest(body,
						exchange.getRequest().getHeaders().getFirst("Host")))));
	}

	@PostMapping("/user/register")
	public Mono<ResponseEntity<? extends Result<?>>> register(@RequestBody Map<String, Object> body,
			ServerWebExchange exchange) {
		return submit(() -> TinyEngineWebResponses.ok(userSystemService.register(
				TinyEngineUserRequestMapper.toRegistrationRequest(body,
						exchange.getRequest().getHeaders().getFirst("Host")))));
	}

	@PostMapping("/user/forgot-password")
	public Mono<ResponseEntity<? extends Result<?>>> forgotPassword(@RequestBody Map<String, Object> body) {
		return submit(() -> {
			userSystemService.requestPasswordReset(TinyEngineUserRequestMapper.toPasswordResetRequest(body));
			return TinyEngineWebResponses.ok();
		});
	}

	@PostMapping("/user/logout")
	public Mono<ResponseEntity<? extends Result<?>>> logout(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
		return submit(() -> {
			userSystemService.logout(credentialExtractor.extract(authorization, tenantId));
			return TinyEngineWebResponses.ok();
		});
	}

	@GetMapping("/user/me")
	public Mono<ResponseEntity<? extends Result<?>>> me(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
		return submitWithPrincipal(authorization, tenantId, principal -> TinyEngineWebResponses.ok(principal));
	}

	@GetMapping("/tenant/list")
	public Mono<ResponseEntity<? extends Result<?>>> tenants(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
		return submitWithPrincipal(authorization, tenantId,
					principal -> TinyEngineWebResponses.ok(userSystemService.tenants(principal)));
	}

	@GetMapping("/user/tenant")
	public Mono<ResponseEntity<? extends Result<?>>> tenant(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String currentTenantId,
			@RequestParam String tenantId) {
		return submitWithPrincipal(authorization, currentTenantId,
				principal -> TinyEngineWebResponses.ok(userSystemService.switchTenant(principal, tenantId)));
	}

	@PostMapping("/tenant/create")
	public Mono<ResponseEntity<? extends Result<?>>> createTenant(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
			@RequestBody Map<String, Object> body) {
		return submitWithPrincipal(authorization, tenantId, principal -> TinyEngineWebResponses.ok(
				userSystemService.createTenant(principal, TinyEngineUserRequestMapper.toTenantCreateRequest(body))));
	}

	@PostMapping("/tenant/switch")
	public Mono<ResponseEntity<? extends Result<?>>> switchTenant(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
			@RequestBody Map<String, Object> body) {
		return submitWithPrincipal(authorization, tenantId, principal -> TinyEngineWebResponses.ok(
				userSystemService.switchTenant(principal, TinyEngineUserRequestMapper.string(body, "tenantId"))));
	}

	private Mono<ResponseEntity<? extends Result<?>>> submit(Callable<ResponseEntity<? extends Result<?>>> task) {
		return executor.submit(task).onErrorResume(ex -> Mono.just(TinyEngineWebResponses.error(ex)));
	}

	private Mono<ResponseEntity<? extends Result<?>>> submitWithPrincipal(String authorization, String tenantId,
			TinyEnginePrincipalTask task) {
		return submit(() -> {
			TinyEngineCredential credential = credentialExtractor.extract(authorization, tenantId);
			TinyEnginePrincipal principal = userSystemService.currentUser(credential);
			return userScope.callAs(principal, () -> task.call(principal));
		});
	}

	private interface TinyEnginePrincipalTask {

		ResponseEntity<? extends Result<?>> call(TinyEnginePrincipal principal);
	}
}
