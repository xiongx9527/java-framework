package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.ApiInfo;

public class MagicApiSentinelResourceResolver {

	private static final String PREFIX = "magicapi:";

	public String resolve(ApiInfo info) {
		if (info == null) {
			return PREFIX + "unknown";
		}
		if (StringUtils.isNotBlank(info.getId())) {
			return PREFIX + info.getId();
		}
		return PREFIX
				+ StringUtils.defaultIfBlank(info.getMethod(), "UNKNOWN")
				+ ":"
				+ normalizePath(info.getPath());
	}

	private String normalizePath(String path) {
		String value = StringUtils.defaultIfBlank(path, "unknown");
		return value.startsWith("/") ? value : "/" + value;
	}
}
