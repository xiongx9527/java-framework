package org.ssssssss.magicapi.core.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

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

	/**
	 * 是否启用管理员模式
	 */
	private Boolean adminEnabled = false;

	private Map<String, Object> attributes = new HashMap<>();

	/**
	 * 构造插件实例
	 *
	 * @param name              插件名
	 * @param globalName        js全局变量名
	 * @param javascriptFilename js文件名
	 */
	public Plugin(String name, String globalName, String javascriptFilename) {
		this.name = name;
		this.globalName = globalName;
		this.javascriptFilename = javascriptFilename;
	}

	/**
	 * 构造插件实例
	 *
	 * @param name       插件名
	 * @param globalName js全局变量名
	 */
	public Plugin(String name, String globalName) {
		this.name = name;
		this.globalName = globalName;
	}

	/**
	 * 构造插件实例
	 *
	 * @param name 插件名
	 */
	public Plugin(String name) {
		this.name = name;
	}
}
