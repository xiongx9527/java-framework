package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.api;

/**
 * Query parameters used by the official TinyEngine code generation endpoint.
 */
public class TinyEngineCodeQuery {

	private Integer platform;
	private Integer app;
	private String type;
	private Integer id;
	private Integer history;

	public Integer getPlatform() {
		return platform;
	}

	public void setPlatform(Integer platform) {
		this.platform = platform;
	}

	public Integer getApp() {
		return app;
	}

	public void setApp(Integer app) {
		this.app = app;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getHistory() {
		return history;
	}

	public void setHistory(Integer history) {
		this.history = history;
	}
}
