package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DataChangeExecutionContextHolderTest {

	@Test
	void restoresOuterContextWhenNestedScopeCloses() {
		DataChangeExecutionContext outer = new DataChangeExecutionContext(null, null, null);
		DataChangeExecutionContext inner = new DataChangeExecutionContext(null, null, null);

		try (DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(outer)) {
			assertThat(DataChangeExecutionContextHolder.current()).containsSame(outer);
			try (DataChangeExecutionContextHolder.Scope nested = DataChangeExecutionContextHolder.push(inner)) {
				assertThat(DataChangeExecutionContextHolder.current()).containsSame(inner);
			}
			assertThat(DataChangeExecutionContextHolder.current()).containsSame(outer);
		}

		assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
	}

	@Test
	void removesThreadLocalWhenScopeClosesAfterException() {
		try {
			try (DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(
					new DataChangeExecutionContext(null, null, null))) {
				throw new IllegalStateException("expected");
			}
		} catch (IllegalStateException ignored) {
			// Expected test fixture failure.
		}

		assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
	}
}
