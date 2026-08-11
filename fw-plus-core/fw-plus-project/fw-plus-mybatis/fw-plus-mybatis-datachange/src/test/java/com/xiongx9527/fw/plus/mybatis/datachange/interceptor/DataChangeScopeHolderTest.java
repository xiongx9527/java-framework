package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class DataChangeScopeHolderTest {

	@Test
	void suppressesInsideScopeAndRestoresAfterClose() {
		try (DataChangeScope ignored = DataChangeScope.suppress()) {
			assertThat(DataChangeScopeHolder.isSuppressed()).isTrue();
		}

		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void nestedScopesRestoreOnlyAfterOutermostClose() {
		DataChangeScope outer = DataChangeScope.suppress();
		DataChangeScope inner = DataChangeScope.suppress();

		inner.close();
		assertThat(DataChangeScopeHolder.isSuppressed()).isTrue();
		outer.close();

		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void closeIsIdempotent() {
		DataChangeScope scope = DataChangeScope.suppress();
		scope.close();
		scope.close();

		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void tryWithResourcesRestoresAfterException() {
		try {
			try (DataChangeScope ignored = DataChangeScope.suppress()) {
				throw new IllegalStateException("boom");
			}
		} catch (IllegalStateException ignored) {
		}

		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void suppressionIsIsolatedToCurrentThread() throws InterruptedException {
		final AtomicBoolean childSuppressed = new AtomicBoolean(true);
		try (DataChangeScope ignored = DataChangeScope.suppress()) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() { childSuppressed.set(DataChangeScopeHolder.isSuppressed()); }
			});
			thread.start();
			thread.join();
		}

		assertThat(childSuppressed).isFalse();
	}
}
