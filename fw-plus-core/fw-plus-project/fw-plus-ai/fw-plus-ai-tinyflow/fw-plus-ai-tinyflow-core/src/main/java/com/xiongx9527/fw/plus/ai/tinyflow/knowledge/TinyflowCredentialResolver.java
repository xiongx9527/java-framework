package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.Arrays;

public interface TinyflowCredentialResolver {

	ResolvedCredential resolve(String tenantId, String credentialRef);

	final class ResolvedCredential {
		private final char[] value;
		private final long expiresAtEpochMillis;

		public ResolvedCredential(char[] value, long expiresAtEpochMillis) {
			if (value == null || value.length == 0) {
				throw new IllegalArgumentException("value must not be empty");
			}
			this.value = Arrays.copyOf(value, value.length);
			this.expiresAtEpochMillis = expiresAtEpochMillis;
		}

		public char[] getValue() { return Arrays.copyOf(value, value.length); }
		public long getExpiresAtEpochMillis() { return expiresAtEpochMillis; }
	}
}
