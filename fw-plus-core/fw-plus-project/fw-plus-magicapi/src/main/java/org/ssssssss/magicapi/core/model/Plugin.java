package org.ssssssss.magicapi.core.model;

import lombok.Data;

@Data
public class Plugin {

	/**
	 * 插件名
	 */
	private final String name;

	/**
	 * js全局变量名
	 */
	private String globalName;

	/**
	 * js文件名
	 */
	private String javascriptFilename;

	public Plugin(String name, String globalName, String javascriptFilename) {
		this.name = name;
		this.globalName = globalName;
		this.javascriptFilename = javascriptFilename;
	}

	public Plugin(String name, String globalName) {
		this.name = name;
		this.globalName = globalName;
	}

	public Plugin(String name) {
		this.name = name;
	}
}
