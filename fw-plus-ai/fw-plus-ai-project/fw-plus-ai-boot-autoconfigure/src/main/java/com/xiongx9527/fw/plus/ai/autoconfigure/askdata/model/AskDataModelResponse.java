package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

public class AskDataModelResponse {

	private final String text;
	private final Object rawResponse;
	private final String modelKey;

	public AskDataModelResponse(String text, Object rawResponse, String modelKey) {
		this.text = text;
		this.rawResponse = rawResponse;
		this.modelKey = modelKey;
	}

	public String getText() {
		return text;
	}

	public Object getRawResponse() {
		return rawResponse;
	}

	public String getModelKey() {
		return modelKey;
	}
}
