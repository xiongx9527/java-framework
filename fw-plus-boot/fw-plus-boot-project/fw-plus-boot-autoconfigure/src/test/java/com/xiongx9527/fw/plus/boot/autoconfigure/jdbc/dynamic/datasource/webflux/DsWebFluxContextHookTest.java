package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsHeaderProcessor;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DsWebFluxContextHookTest {

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
	void exposesSnapshotDuringSynchronousSignalAndClearsAfterward() {
		DsRequestContext context = headerContext("tenant_a");

		String result = Mono.just("trigger")
				.map(value -> new DsHeaderProcessor().doDetermineDatasource(null, "#header.ds"))
				.contextWrite(reactorContext -> reactorContext.put(DsWebFluxContextHook.CONTEXT_KEY, context))
				.block();

		assertEquals("tenant_a", result);
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void keepsConcurrentSnapshotsIsolatedAcrossSchedulerSwitch() {
		Mono<String> first = readHeaderOnParallelScheduler(headerContext("tenant_a"));
		Mono<String> second = readHeaderOnParallelScheduler(headerContext("tenant_b"));

		Tuple2<String, String> result = Mono.zip(first, second).block();

		assertEquals("tenant_a", result.getT1());
		assertEquals("tenant_b", result.getT2());
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void clearsSnapshotWhenSignalCallbackThrows() {
		DsRequestContext context = headerContext("tenant_error");

		assertThrows(IllegalStateException.class, () -> Mono.just("trigger")
				.map(value -> {
					assertSame(context, DsRequestContextHolder.getContext());
					throw new IllegalStateException("boom");
				})
				.contextWrite(reactorContext -> reactorContext.put(DsWebFluxContextHook.CONTEXT_KEY, context))
				.block());
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void leavesExistingThreadContextUntouchedWhenReactorContextHasNoSnapshot() {
		DsRequestContext existing = headerContext("tenant_existing");

		try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(existing)) {
			DsRequestContext observed = Mono.just("trigger")
					.map(value -> DsRequestContextHolder.getContext())
					.block();

			assertSame(existing, observed);
			assertSame(existing, DsRequestContextHolder.getContext());
		}
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void keepsHookInstalledUntilLastDistinctInstanceIsDestroyed() throws Exception {
		DsWebFluxContextHook second = new DsWebFluxContextHook();
		second.afterPropertiesSet();
		try {
			hook.destroy();
			hook.destroy();

			assertEquals("tenant_second", readHeader(headerContext("tenant_second")));
		} finally {
			second.destroy();
		}

		assertEquals("missing", readHeader(headerContext("tenant_removed")));
	}

	@Test
	void repeatedInitializationOfSameInstanceDoesNotIncreaseReferenceCount() throws Exception {
		hook.afterPropertiesSet();

		hook.destroy();

		assertEquals("missing", readHeader(headerContext("tenant_removed")));
	}

	private Mono<String> readHeaderOnParallelScheduler(DsRequestContext context) {
		return Mono.just("trigger")
				.publishOn(Schedulers.parallel())
				.map(value -> new DsHeaderProcessor().doDetermineDatasource(null, "#header.ds"))
				.contextWrite(reactorContext -> reactorContext.put(DsWebFluxContextHook.CONTEXT_KEY, context));
	}

	private String readHeader(DsRequestContext context) {
		return Mono.just("trigger")
				.map(value -> {
					String datasource = new DsHeaderProcessor().doDetermineDatasource(null, "#header.ds");
					return datasource == null ? "missing" : datasource;
				})
				.contextWrite(reactorContext -> reactorContext.put(DsWebFluxContextHook.CONTEXT_KEY, context))
				.block();
	}

	private DsRequestContext headerContext(String datasource) {
		return new DsRequestContext(
				Collections.singletonMap("ds", Collections.singletonList(datasource)),
				Collections.<String, Object>emptyMap());
	}

}
