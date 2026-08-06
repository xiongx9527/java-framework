package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 动态数据源请求信息快照。
 */
public final class DsRequestContext {

	private final Map<String, List<String>> headers;
	private final Map<String, Object> sessionAttributes;

	public DsRequestContext(Map<String, ? extends List<String>> headers,
							Map<String, ?> sessionAttributes) {
		Map<String, List<String>> headerCopies = new LinkedHashMap<>();
		if (headers != null) {
			for (Map.Entry<String, ? extends List<String>> entry : headers.entrySet()) {
				List<String> values = entry.getValue();
				headerCopies.put(entry.getKey().toLowerCase(Locale.ENGLISH),
						values == null ? Collections.<String>emptyList()
								: Collections.unmodifiableList(new ArrayList<>(values)));
			}
		}
		this.headers = Collections.unmodifiableMap(headerCopies);

		Map<String, Object> sessionAttributeCopies = new LinkedHashMap<>();
		if (sessionAttributes != null) {
			sessionAttributeCopies.putAll(sessionAttributes);
		}
		this.sessionAttributes = Collections.unmodifiableMap(sessionAttributeCopies);
	}

	public String getHeader(String name) {
		List<String> values = headers.get(name.toLowerCase(Locale.ENGLISH));
		return values == null || values.isEmpty() ? null : values.get(0);
	}

	public Object getSessionAttribute(String name) {
		return sessionAttributes.get(name);
	}

}
