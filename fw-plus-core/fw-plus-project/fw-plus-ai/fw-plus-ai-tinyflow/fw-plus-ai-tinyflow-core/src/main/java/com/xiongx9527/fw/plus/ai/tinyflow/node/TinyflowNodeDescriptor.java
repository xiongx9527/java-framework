package com.xiongx9527.fw.plus.ai.tinyflow.node;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TinyflowNodeDescriptor {

	private final String type;
	private final String name;
	private final String description;
	private final Map<String, Object> properties;

	public TinyflowNodeDescriptor(String type, String name, String description, Map<String, Object> properties) {
		this.type = type;
		this.name = name;
		this.description = description;
		this.properties = properties == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(properties));
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}
}
