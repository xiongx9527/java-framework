package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.options;

public class MagicS3PlatformOption {

	private String text;

	private String value;

	public MagicS3PlatformOption() {
	}

	public MagicS3PlatformOption(String text, String value) {
		this.text = text;
		this.value = value;
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
}
