package com.xiongx9527.fw.plus.tinyengine.common.util;

import java.util.HashMap;
import java.util.Map;

public final class TinyEngineSchemaUtils {

	private TinyEngineSchemaUtils() {
	}

	public static Map<String, Object> getFolderSchema(Map<String, Object> data) {
		Map<String, Object> schema = copy(data);
		schema.put("componentName", "Folder");
		return schema;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> getSchemaBase(Map<String, Object> data) {
		Map<String, Object> pageContent = data == null ? null : (Map<String, Object>) data.get("page_content");
		Map<String, Object> schema = copy(pageContent);
		if (data != null) {
			schema.put("fileName", data.get("name"));
		}
		return schema;
	}

	private static Map<String, Object> copy(Map<String, Object> data) {
		return data == null ? new HashMap<String, Object>() : new HashMap<String, Object>(data);
	}
}
