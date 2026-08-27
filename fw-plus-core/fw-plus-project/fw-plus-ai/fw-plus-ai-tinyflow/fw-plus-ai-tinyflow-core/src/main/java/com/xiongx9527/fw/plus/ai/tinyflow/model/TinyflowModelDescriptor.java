package com.xiongx9527.fw.plus.ai.tinyflow.model;

public final class TinyflowModelDescriptor {

	private final String key;
	private final String name;
	private final String type;
	private final String provider;

	public TinyflowModelDescriptor(String key, String name, String type, String provider) {
		this.key = key;
		this.name = name;
		this.type = type;
		this.provider = provider;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getProvider() {
		return provider;
	}
}
