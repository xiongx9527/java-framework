package com.xiongx9527.fw.plus.tinyengine.support.context;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TinyEngineRequestUserScopeTest {

	private final TinyEngineRequestUserScope scope = new TinyEngineRequestUserScope();

	@Test
	void exposesCurrentUserOnlyInsideScopeAndCleansAfterSuccess() throws Exception {
		TinyEnginePrincipal principal = principal("u1", "tenant-1");

		String userId = scope.callAs(principal, () -> scope.getCurrentUser().getUserId());

		assertThat(userId).isEqualTo("u1");
		assertThat(scope.getCurrentUser()).isNull();
	}

	@Test
	void cleansAfterExceptionAndRestoresOuterPrincipal() throws Exception {
		TinyEnginePrincipal outer = principal("outer", "tenant-1");
		TinyEnginePrincipal inner = principal("inner", "tenant-2");

		assertThatThrownBy(() -> scope.callAs(outer, () -> {
			assertThat(scope.getCurrentUser().getUserId()).isEqualTo("outer");
			return scope.callAs(inner, () -> {
				assertThat(scope.getCurrentUser().getUserId()).isEqualTo("inner");
				throw new IllegalStateException("boom");
			});
		})).isInstanceOf(IllegalStateException.class);

		assertThat(scope.getCurrentUser()).isNull();
	}

	@Test
	void isolatesConcurrentPrincipals() throws Exception {
		ExecutorService executorService = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch release = new CountDownLatch(1);
		try {
			Future<String> first = executorService.submit(() -> currentUserWhileBothRequestsOverlap(
					principal("u1", "tenant-1"), ready, release));
			Future<String> second = executorService.submit(() -> currentUserWhileBothRequestsOverlap(
					principal("u2", "tenant-2"), ready, release));
			ready.await();
			release.countDown();

			List<String> userIds = Arrays.asList(first.get(), second.get());

			assertThat(userIds).containsExactlyInAnyOrder("u1", "u2");
		} finally {
			executorService.shutdownNow();
		}
	}

	private String currentUserWhileBothRequestsOverlap(TinyEnginePrincipal principal, CountDownLatch ready,
			CountDownLatch release) throws Exception {
		return scope.callAs(principal, () -> {
			ready.countDown();
			release.await();
			return scope.getCurrentUser().getUserId();
		});
	}

	private static TinyEnginePrincipal principal(String userId, String tenantId) {
		return new TinyEnginePrincipal(userId, userId, tenantId, tenantId, Integer.valueOf(1));
	}
}
