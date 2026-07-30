package com.xiongx9527.fw.plus.tinyengine.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

public final class TinyEngineJsonUtils {

	private TinyEngineJsonUtils() {
	}

	public static String toJson(Object value) {
		return JSON.toJSONString(value);
	}

	public static JSONObject parseObject(String json) {
		if (isBlank(json)) {
			return new JSONObject();
		}
		return JSON.parseObject(json);
	}

	public static JSONArray parseArray(String json) {
		if (isBlank(json)) {
			return new JSONArray();
		}
		return JSON.parseArray(json);
	}

	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
