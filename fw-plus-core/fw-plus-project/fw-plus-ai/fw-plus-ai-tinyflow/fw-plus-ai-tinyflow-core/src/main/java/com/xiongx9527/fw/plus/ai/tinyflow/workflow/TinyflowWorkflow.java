package com.xiongx9527.fw.plus.ai.tinyflow.workflow;

public final class TinyflowWorkflow {

	private final String id;
	private final String path;
	private final String name;
	private final String json;

	public TinyflowWorkflow(String id, String path, String name, String json) {
		this.id = requireNonBlank(id, "id");
		this.path = requireNonBlank(path, "path");
		this.name = name;
		this.json = requireNonBlankVerbatim(json, "json");
	}

	public String getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public String getName() {
		return name;
	}

	public String getJson() {
		return json;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value.trim();
	}

	private static String requireNonBlankVerbatim(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
