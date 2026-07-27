package com.xiongx9527.fw.plus.tinyengine.common.util;

import java.io.File;
import java.util.Locale;

public final class TinyEngineFileSecurityUtils {

	private TinyEngineFileSecurityUtils() {
	}

	public static boolean checkPathHasCrossDir(String path) {
		if (TinyEngineJsonUtils.isBlank(path)) {
			return true;
		}
		String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
		return normalized.startsWith("/") || normalized.contains("../") || normalized.equals("..")
				|| normalized.contains("/..") || normalized.contains("./") || normalized.equals(".")
				|| normalized.contains("%00");
	}

	public static void validateRelativePath(String path) {
		if (checkPathHasCrossDir(path) || new File(path).isAbsolute()) {
			throw new IllegalArgumentException("Invalid relative file path");
		}
	}
}
