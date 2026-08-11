package com.xiongx9527.fw.plus.ai.tinyflow.vector;

public final class TinyflowVectorStoreDescriptor {

	private final String key;
	private final String name;
	private final String type;
	private final String provider;
	private final boolean metadataFilterSupported;

	public TinyflowVectorStoreDescriptor(String key, String name, String type, String provider,
			boolean metadataFilterSupported) {
		this.key = key;
		this.name = name;
		this.type = type;
		this.provider = provider;
		this.metadataFilterSupported = metadataFilterSupported;
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

	public boolean isMetadataFilterSupported() {
		return metadataFilterSupported;
	}
}
