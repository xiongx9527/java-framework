package com.xiongx9527.fw.plus.tinyengine.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TinyEngineHashUtils {

	private TinyEngineHashUtils() {
	}

	public static String calculateMD5Hash(Map<String, Object> value) {
		if (value == null) {
			return "";
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			byte[] bytes = digest.digest(canonicalize(value).getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(bytes.length * 2);
			for (byte valueByte : bytes) {
				result.append(String.format("%02x", valueByte & 0xff));
			}
			return result.toString();
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("MD5 algorithm is unavailable", exception);
		}
	}

	private static String canonicalize(Object value) {
		if (value == null) {
			return "null";
		}
		if (value instanceof Map) {
			return canonicalizeMap((Map<?, ?>) value);
		}
		if (value instanceof Collection) {
			return canonicalizeCollection((Collection<?>) value);
		}
		if (value.getClass().isArray()) {
			return canonicalizeArray(value);
		}
		return "value" + encode(value.getClass().getName()) + encode(String.valueOf(value));
	}

	private static String canonicalizeMap(Map<?, ?> value) {
		List<String> entries = new ArrayList<String>(value.size());
		for (Map.Entry<?, ?> entry : value.entrySet()) {
			entries.add(encode(canonicalize(entry.getKey())) + encode(canonicalize(entry.getValue())));
		}
		Collections.sort(entries);
		return "map" + encodeSequence(entries);
	}

	private static String canonicalizeCollection(Collection<?> value) {
		List<String> elements = new ArrayList<String>(value.size());
		for (Object element : value) {
			elements.add(canonicalize(element));
		}
		if (value instanceof Set) {
			Collections.sort(elements);
			return "set" + encodeSequence(elements);
		}
		return "collection" + encodeSequence(elements);
	}

	private static String canonicalizeArray(Object value) {
		int length = Array.getLength(value);
		List<String> elements = new ArrayList<String>(length);
		for (int index = 0; index < length; index++) {
			elements.add(canonicalize(Array.get(value, index)));
		}
		return "array" + encodeSequence(elements);
	}

	private static String encodeSequence(List<String> values) {
		StringBuilder result = new StringBuilder();
		result.append('[');
		for (String value : values) {
			result.append(encode(value));
		}
		result.append(']');
		return result.toString();
	}

	private static String encode(String value) {
		return value.length() + ":" + value;
	}
}
