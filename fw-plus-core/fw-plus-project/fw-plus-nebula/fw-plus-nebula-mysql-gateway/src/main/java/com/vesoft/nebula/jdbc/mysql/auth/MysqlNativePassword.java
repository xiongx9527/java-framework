package com.vesoft.nebula.jdbc.mysql.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class MysqlNativePassword {
	private MysqlNativePassword() {
	}

	public static byte[] scramble(String password, byte[] seed) {
		if (password == null || password.length() == 0) {
			return new byte[0];
		}
		byte[] passwordHash = sha1(password.getBytes());
		byte[] doubleHash = sha1(passwordHash);
		byte[] seedAndHash = new byte[(seed == null ? 0 : seed.length) + doubleHash.length];
		if (seed != null) {
			System.arraycopy(seed, 0, seedAndHash, 0, seed.length);
		}
		System.arraycopy(doubleHash, 0, seedAndHash, seed == null ? 0 : seed.length, doubleHash.length);
		byte[] digest = sha1(seedAndHash);
		byte[] token = new byte[passwordHash.length];
		for (int i = 0; i < passwordHash.length; i++) {
			token[i] = (byte) (passwordHash[i] ^ digest[i]);
		}
		return token;
	}

	public static boolean matches(String password, byte[] seed, byte[] response) {
		return Arrays.equals(scramble(password, seed), response == null ? new byte[0] : response);
	}

	private static byte[] sha1(byte[] value) {
		try {
			return MessageDigest.getInstance("SHA-1").digest(value);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-1 digest is required by mysql_native_password.", exception);
		}
	}
}
