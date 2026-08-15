package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TinyEngineReactiveAuthenticationFilterTest {

	@Test
	void authenticatesProtectedRequestAndRestoresPrincipalOnBlockingWorker() {
		TinyEngineRequestUserScope userScope = new TinyEngineRequestUserScope();
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(Schedulers.immediate(),
				Duration.ofSeconds(1), userScope);
		TinyEngineUserSystemService userSystemService = mock(TinyEngineUserSystemService.class);
		TinyEnginePrincipal principal = new TinyEnginePrincipal("u1", "User One", "tenant-1", "renter-1", 1);
		when(userSystemService.currentUser(any(TinyEngineCredential.class))).thenReturn(principal);
		TinyEngineReactiveAuthenticationFilter filter = new TinyEngineReactiveAuthenticationFilter(
				new TinyEngineCredentialExtractor(), userSystemService, executor);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
				.get("/app-center/api/apps")
				.header("Authorization", "Bearer token")
				.header("X-Tenant-Id", "tenant-1"));
		AtomicReference<Object> observed = new AtomicReference<Object>();

		filter.filter(exchange, current -> executor.submit(() -> {
			observed.set(userScope.getCurrentUser());
			return "ok";
		}).then()).block();

		assertSame(principal, observed.get());
		assertEquals(null, userScope.getCurrentUser());
	}

	@Test
	void rejectsProtectedRequestWithoutLoginTokenButAllowsLoginEndpoint() {
		TinyEngineRequestUserScope userScope = new TinyEngineRequestUserScope();
		TinyEngineReactiveExecutor executor = new TinyEngineReactiveExecutor(Schedulers.immediate(),
				Duration.ofSeconds(1), userScope);
		TinyEngineUserSystemService userSystemService = mock(TinyEngineUserSystemService.class);
		TinyEngineReactiveAuthenticationFilter filter = new TinyEngineReactiveAuthenticationFilter(
				new TinyEngineCredentialExtractor(), userSystemService, executor);
		MockServerWebExchange protectedExchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/tiny-engine/app-center/api/chat/completions"));

		filter.filter(protectedExchange, current -> Mono.empty()).block();

		assertEquals(HttpStatus.UNAUTHORIZED, protectedExchange.getResponse().getStatusCode());

		MockServerWebExchange loginExchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/platform-center/api/user/login"));
		AtomicReference<Boolean> invoked = new AtomicReference<Boolean>(false);
		filter.filter(loginExchange, current -> {
			invoked.set(true);
			return Mono.empty();
		}).block();
		assertEquals(Boolean.TRUE, invoked.get());
	}
}
