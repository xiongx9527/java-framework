package com.xiongx9527.fw.plus.tinyengine.common.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

public final class TinyEngineFileSecurityUtils {

	private TinyEngineFileSecurityUtils() {
	}

	public static boolean checkPathHasCrossDir(String path) {
		if (TinyEngineJsonUtils.isBlank(path)) {
			return true;
		}
		String normalized = decode(path).replace('\\', '/').toLowerCase(Locale.ROOT);
		return normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")
				|| normalized.contains("/..") || normalized.contains("./") || normalized.equals(".")
				|| normalized.contains("%00") || normalized.indexOf('\0') >= 0
				|| normalized.matches("^[a-z]:/.*");
	}

	public static void validateRelativePath(String path) {
		if (checkPathHasCrossDir(path) || new File(path).isAbsolute()) {
			throw new IllegalArgumentException("Invalid relative file path");
		}
	}

	private static String decode(String path) {
		String decoded = path;
		try {
			for (int index = 0; index < 2; index++) {
				String next = URLDecoder.decode(decoded, "UTF-8");
				if (next.equals(decoded)) {
					break;
				}
				decoded = next;
			}
			return decoded;
		}
		catch (IllegalArgumentException exception) {
			return "../";
		}
		catch (UnsupportedEncodingException exception) {
			throw new IllegalStateException(exception);
		}
	}
}
