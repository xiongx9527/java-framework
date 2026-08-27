package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsHeaderProcessor;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsSessionProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DsWebFluxRequestContextFilterTest {

	private DsWebFluxContextHook hook;

	@BeforeEach
	void setUp() throws Exception {
		hook = new DsWebFluxContextHook();
		hook.afterPropertiesSet();
	}

	@AfterEach
	void tearDown() throws Exception {
		hook.destroy();
		DsRequestContextHolder.clear();
	}

	@Test
	void exposesHeaderAndSessionToProcessors() {
		MockWebSession session = new MockWebSession();
		session.getAttributes().put("ds", "tenant_session");
		MockServerWebExchange exchange = MockServerWebExchange.builder(
				MockServerHttpRequest.get("/test").header("X-DS", "tenant_header"))
				.session(session)
				.build();
		AtomicReference<String> header = new AtomicReference<String>();
		AtomicReference<String> sessionValue = new AtomicReference<String>();

		WebFilterChain chain = current -> Mono.just("trigger").map(value -> {
			header.set(new DsHeaderProcessor().doDetermineDatasource(null, "#header.X-DS"));
			sessionValue.set(new DsSessionProcessor().doDetermineDatasource(null, "#session.ds"));
			return value;
		}).then();

		new DsWebFluxRequestContextFilter().filter(exchange, chain).block();

		assertEquals("tenant_header", header.get());
		assertEquals("tenant_session", sessionValue.get());
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void capturesSessionAttributesBeforeDownstreamMutation() {
		MockWebSession session = new MockWebSession();
		session.getAttributes().put("ds", "tenant_snapshot");
		MockServerWebExchange exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("/test"))
				.session(session)
				.build();
		AtomicReference<String> observed = new AtomicReference<String>();
		WebFilterChain chain = current -> Mono.just("trigger").map(value -> {
			session.getAttributes().put("ds", "tenant_mutated");
			observed.set(new DsSessionProcessor().doDetermineDatasource(null, "#session.ds"));
			return value;
		}).then();

		new DsWebFluxRequestContextFilter().filter(exchange, chain).block();

		assertEquals("tenant_snapshot", observed.get());
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void clearsSnapshotWhenDownstreamFails() {
		MockWebSession session = new MockWebSession();
		session.getAttributes().put("ds", "tenant_error");
		MockServerWebExchange exchange = MockServerWebExchange.builder(MockServerHttpRequest.get("/test"))
				.session(session)
				.build();
		WebFilterChain chain = current -> Mono.just("trigger").map(value -> {
			assertEquals("tenant_error",
					new DsSessionProcessor().doDetermineDatasource(null, "#session.ds"));
			throw new IllegalStateException("boom");
		}).then();

		assertThrows(IllegalStateException.class,
				() -> new DsWebFluxRequestContextFilter().filter(exchange, chain).block());
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void runsAtHighestPrecedence() {
		assertEquals(Ordered.HIGHEST_PRECEDENCE, new DsWebFluxRequestContextFilter().getOrder());
	}

}
