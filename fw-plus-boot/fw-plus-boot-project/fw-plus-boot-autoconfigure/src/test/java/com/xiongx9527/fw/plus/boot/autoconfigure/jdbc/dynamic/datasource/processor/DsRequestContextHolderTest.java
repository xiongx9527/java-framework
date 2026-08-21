package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class DsRequestContextHolderTest {

	@AfterEach
	void tearDown() {
		DsRequestContextHolder.clear();
	}

	@Test
	void nestedScopeRestoresPreviousContext() {
		DsRequestContext outer = context("outer");
		DsRequestContext inner = context("inner");
		try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(outer)) {
			assertSame(outer, DsRequestContextHolder.getContext());
			try (DsRequestContextHolder.Scope nested = DsRequestContextHolder.open(inner)) {
				assertSame(inner, DsRequestContextHolder.getContext());
			}
			assertSame(outer, DsRequestContextHolder.getContext());
		}
		assertNull(DsRequestContextHolder.getContext());
	}

	@Test
	void repeatedCloseDoesNotRestoreContextTwice() {
		DsRequestContext outer = context("outer");
		DsRequestContext inner = context("inner");
		try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(outer)) {
			DsRequestContextHolder.Scope nested = DsRequestContextHolder.open(inner);
			nested.close();
			nested.close();
			assertSame(outer, DsRequestContextHolder.getContext());
		}
	}

	private DsRequestContext context(String datasource) {
		return new DsRequestContext(Collections.<String, java.util.List<String>>emptyMap(),
				Collections.<String, Object>singletonMap("ds", datasource));
	}

}
