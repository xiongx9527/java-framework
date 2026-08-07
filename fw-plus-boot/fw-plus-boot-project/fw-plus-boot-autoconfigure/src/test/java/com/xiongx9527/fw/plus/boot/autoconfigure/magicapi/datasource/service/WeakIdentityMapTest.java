package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;

import static org.assertj.core.api.Assertions.assertThat;

class WeakIdentityMapTest {

	@Test
	void keepsEqualKeysAsDistinctIdentityEntries() {
		WeakIdentityMap<EqualKey, String> map = new WeakIdentityMap<EqualKey, String>();
		EqualKey first = new EqualKey("orders");
		EqualKey second = new EqualKey("orders");

		map.put(first, "first-stage");
		map.put(second, "second-stage");

		assertThat(map.get(first)).isEqualTo("first-stage");
		assertThat(map.get(second)).isEqualTo("second-stage");
		assertThat(map.size()).isEqualTo(2);
	}

	@Test
	void explicitlyEnqueuedKeyIsPurgedWithoutSystemGc() {
		WeakIdentityMap<Object, String> map = new WeakIdentityMap<Object, String>();
		Object node = new Object();
		WeakReference<Object> trackedKey = map.put(node, "stages");

		assertThat(trackedKey.enqueue()).isTrue();
		assertThat(map.purgeStaleEntries()).isOne();
		assertThat(map.size()).isZero();
	}

	@Test
	void readsAutomaticallyPurgeExplicitlyEnqueuedKeys() {
		WeakIdentityMap<Object, String> map = new WeakIdentityMap<Object, String>();
		Object node = new Object();
		WeakReference<Object> trackedKey = map.put(node, "stages");

		assertThat(trackedKey.enqueue()).isTrue();

		assertThat(map.get(node)).isNull();
		assertThat(map.size()).isZero();
	}

	private static final class EqualKey {

		private final String value;

		private EqualKey(String value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object other) {
			return other instanceof EqualKey && value.equals(((EqualKey) other).value);
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}
}
