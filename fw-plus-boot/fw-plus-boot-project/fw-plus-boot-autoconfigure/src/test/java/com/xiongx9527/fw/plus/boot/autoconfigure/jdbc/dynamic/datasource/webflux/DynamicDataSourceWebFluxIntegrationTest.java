package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsHeaderProcessor;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContextHolder;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsSessionProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebFilter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicDataSourceWebFluxIntegrationTest {

	private DsWebFluxContextHook hook;
	private WebTestClient client;

	@BeforeEach
	void setUp() {
		hook = new DsWebFluxContextHook();
		hook.afterPropertiesSet();
		WebFilter sessionSeed = (exchange, chain) -> exchange.getSession()
				.doOnNext(session -> session.getAttributes().put("ds", "tenant_b"))
				.then(chain.filter(exchange));
		client = WebTestClient.bindToController(new TestController())
				.webFilter(sessionSeed, new DsWebFluxRequestContextFilter())
				.build();
	}

	@AfterEach
	void tearDown() {
		hook.destroy();
		DsRequestContextHolder.clear();
	}

	@Test
	void resolvesHeaderAndSessionInsideSynchronousController() {
		assertThat(response("/header", "tenant_a")).isEqualTo("tenant_a");
		assertThat(response("/session", "ignored")).isEqualTo("tenant_b");
		assertThat(DsRequestContextHolder.getContext()).isNull();
	}

	@Test
	void concurrentRequestsDoNotShareHeaderValues() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<String> first = executor.submit(() -> response("/header", "tenant_a"));
			Future<String> second = executor.submit(() -> response("/header", "tenant_b"));
			assertThat(first.get()).isEqualTo("tenant_a");
			assertThat(second.get()).isEqualTo("tenant_b");
		} finally {
			executor.shutdownNow();
		}
		assertThat(DsRequestContextHolder.getContext()).isNull();
	}

	private String response(String path, String datasource) {
		return client.get().uri(path).header("X-DS", datasource)
				.exchange()
				.expectStatus().isOk()
				.expectBody(String.class)
				.returnResult()
				.getResponseBody();
	}

	@RestController
	static class TestController {

		@GetMapping(value = "/header", produces = MediaType.TEXT_PLAIN_VALUE)
		String header() {
			return new DsHeaderProcessor().doDetermineDatasource(null, "#header.X-DS");
		}

		@GetMapping(value = "/session", produces = MediaType.TEXT_PLAIN_VALUE)
		String session() {
			return new DsSessionProcessor().doDetermineDatasource(null, "#session.ds");
		}

	}

}
