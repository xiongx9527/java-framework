package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options;

import java.util.ArrayList;
import java.util.List;

public class MagicS3PlatformCandidate {

	private String text;

	private String value;

	private String configClassName;

	private List<String> requiredClassNames = new ArrayList<String>();

	public MagicS3PlatformCandidate() {
	}

	public MagicS3PlatformCandidate(String text, String value, String configClassName) {
		this.text = text;
		this.value = value;
		this.configClassName = configClassName;
	}

	public MagicS3PlatformCandidate(String text, String value, String configClassName, List<String> requiredClassNames) {
		this(text, value, configClassName);
		setRequiredClassNames(requiredClassNames);
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getConfigClassName() {
		return configClassName;
	}

	public void setConfigClassName(String configClassName) {
		this.configClassName = configClassName;
	}

	public List<String> getRequiredClassNames() {
		return requiredClassNames;
	}

	public void setRequiredClassNames(List<String> requiredClassNames) {
		this.requiredClassNames = requiredClassNames == null ? new ArrayList<String>() : requiredClassNames;
	}
}
