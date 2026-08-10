package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

final class WeakIdentityMap<K, V> {

	private final ReferenceQueue<K> staleKeys = new ReferenceQueue<K>();
	private final Map<IdentityWeakReference<K>, V> entries =
			new HashMap<IdentityWeakReference<K>, V>();

	WeakReference<K> put(K key, V value) {
		Objects.requireNonNull(key, "key");
		Objects.requireNonNull(value, "value");
		purgeStaleEntries();
		entries.remove(new IdentityWeakReference<K>(key));
		IdentityWeakReference<K> trackedKey =
				new IdentityWeakReference<K>(key, staleKeys);
		entries.put(trackedKey, value);
		return trackedKey;
	}

	V get(K key) {
		Objects.requireNonNull(key, "key");
		purgeStaleEntries();
		return entries.get(new IdentityWeakReference<K>(key));
	}

	V remove(K key) {
		Objects.requireNonNull(key, "key");
		purgeStaleEntries();
		return entries.remove(new IdentityWeakReference<K>(key));
	}

	int size() {
		purgeStaleEntries();
		return entries.size();
	}

	int purgeStaleEntries() {
		int removed = 0;
		Reference<? extends K> staleKey;
		while ((staleKey = staleKeys.poll()) != null) {
			if (entries.containsKey(staleKey)) {
				entries.remove(staleKey);
				removed++;
			}
		}
		return removed;
	}

	private static final class IdentityWeakReference<T> extends WeakReference<T> {

		private final int identityHashCode;

		private IdentityWeakReference(T referent) {
			super(referent);
			identityHashCode = System.identityHashCode(referent);
		}

		private IdentityWeakReference(T referent, ReferenceQueue<T> queue) {
			super(referent, queue);
			identityHashCode = System.identityHashCode(referent);
		}

		@Override
		public int hashCode() {
			return identityHashCode;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IdentityWeakReference)) {
				return false;
			}
			Object referent = get();
			return referent != null
					&& referent == ((IdentityWeakReference<?>) other).get();
		}
	}
}
