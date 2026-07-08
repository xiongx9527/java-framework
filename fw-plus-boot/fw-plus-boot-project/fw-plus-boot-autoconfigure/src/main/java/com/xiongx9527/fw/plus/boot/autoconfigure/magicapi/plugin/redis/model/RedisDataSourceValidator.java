package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class RedisDataSourceValidator {

	private static final Set<String> CLIENT_TYPES = new HashSet<>(Arrays.asList("lettuce", "jedis"));

	private static final Set<String> MODES = new HashSet<>(Arrays.asList("standalone", "cluster", "sentinel"));

	private RedisDataSourceValidator() {
	}

	public static void validate(RedisDataSourceInfo info) {
		if (info == null) {
			throw new IllegalArgumentException("Redis 数据源配置不能为空");
		}
		requireText(info.getName(), "name不能为空");
		requireText(info.getKey(), "key不能为空");

		String clientType = normalizeClientType(info.getClientType());
		if (!CLIENT_TYPES.contains(clientType)) {
			throw new IllegalArgumentException("clientType不支持: " + info.getClientType());
		}

		String mode = normalizeMode(info.getMode());
		if (!MODES.contains(mode)) {
			throw new IllegalArgumentException("mode不支持: " + info.getMode());
		}

		JSONObject other = parseOtherJson(info.getOther());
		validateDatabase(info.getDatabase(), mode);
		if ("standalone".equals(mode)) {
			requireText(info.getHostname(), "hostname不能为空");
			parsePort(info.getPort(), "port");
		} else if ("cluster".equals(mode)) {
			validateClusterNodes(info, other);
		} else if ("sentinel".equals(mode)) {
			validateSentinelNodes(info, other);
		}
	}

	public static String normalizeClientType(String clientType) {
		return StringUtils.defaultIfBlank(clientType, "lettuce").toLowerCase(Locale.ENGLISH);
	}

	public static String normalizeMode(String mode) {
		return StringUtils.defaultIfBlank(mode, "standalone").toLowerCase(Locale.ENGLISH);
	}

	public static JSONObject parseOtherJson(String other) {
		if (StringUtils.isBlank(other)) {
			return new JSONObject();
		}
		try {
			Object parsed = JSON.parse(other);
			if (!(parsed instanceof JSONObject)) {
				throw new IllegalArgumentException("other必须是JSON对象");
			}
			return (JSONObject) parsed;
		} catch (JSONException ex) {
			throw new IllegalArgumentException("other必须是合法JSON对象", ex);
		}
	}

	public static int parsePort(Object value, String fieldName) {
		if (value == null || StringUtils.isBlank(String.valueOf(value))) {
			throw new IllegalArgumentException(fieldName + "不能为空");
		}
		try {
			int port = Integer.parseInt(String.valueOf(value));
			if (port <= 0 || port > 65535) {
				throw new IllegalArgumentException(fieldName + "必须是1-65535之间的有效端口号");
			}
			return port;
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(fieldName + "必须是有效数字: " + value, ex);
		}
	}

	private static void validateDatabase(String database, String mode) {
		if ("cluster".equals(mode) || StringUtils.isBlank(database)) {
			return;
		}
		try {
			int databaseIndex = Integer.parseInt(database);
			if (databaseIndex < 0) {
				throw new IllegalArgumentException("database不能小于0");
			}
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("database必须是有效数字: " + database, ex);
		}
	}

	private static void validateClusterNodes(RedisDataSourceInfo info, JSONObject other) {
		boolean hasNode = false;
		if (StringUtils.isNotBlank(info.getHostname()) || StringUtils.isNotBlank(info.getPort())) {
			requireText(info.getHostname(), "hostname不能为空");
			parsePort(info.getPort(), "port");
			hasNode = true;
		}
		JSONArray nodes = other.getJSONArray("nodes");
		if (nodes != null) {
			for (int i = 0; i < nodes.size(); i++) {
				JSONObject node = nodes.getJSONObject(i);
				requireText(node.getString("host"), "nodes[" + i + "].host不能为空");
				parsePort(node.get("port"), "nodes[" + i + "].port");
				hasNode = true;
			}
		}
		if (!hasNode) {
			throw new IllegalArgumentException("cluster模式至少需要配置hostname/port或other.nodes");
		}
	}

	private static void validateSentinelNodes(RedisDataSourceInfo info, JSONObject other) {
		requireText(other.getString("masterName"), "masterName不能为空");
		boolean hasSentinel = false;
		if (StringUtils.isNotBlank(info.getHostname()) || StringUtils.isNotBlank(info.getPort())) {
			requireText(info.getHostname(), "hostname不能为空");
			parsePort(info.getPort(), "port");
			hasSentinel = true;
		}
		JSONArray sentinels = other.getJSONArray("sentinels");
		if (sentinels != null) {
			for (int i = 0; i < sentinels.size(); i++) {
				JSONObject sentinel = sentinels.getJSONObject(i);
				requireText(sentinel.getString("host"), "sentinels[" + i + "].host不能为空");
				parsePort(sentinel.get("port"), "sentinels[" + i + "].port");
				hasSentinel = true;
			}
		}
		if (!hasSentinel) {
			throw new IllegalArgumentException("sentinel模式至少需要配置hostname/port或other.sentinels");
		}
	}

	private static void requireText(String value, String message) {
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException(message);
		}
	}
}
