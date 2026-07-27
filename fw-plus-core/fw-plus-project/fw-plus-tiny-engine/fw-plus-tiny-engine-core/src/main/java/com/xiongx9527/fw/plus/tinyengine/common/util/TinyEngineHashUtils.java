package com.xiongx9527.fw.plus.tinyengine.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

public final class TinyEngineHashUtils {

	private TinyEngineHashUtils() {
	}

	public static String calculateMD5Hash(Map<String, Object> value) {
		if (value == null) {
			return "";
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] bytes = digest.digest(new TreeMap<String, Object>(value).toString().getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(bytes.length * 2);
			for (byte valueByte : bytes) {
				result.append(String.format("%02x", valueByte & 0xff));
			}
			return result.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("MD5 algorithm is unavailable", exception);
		}
	}
}
