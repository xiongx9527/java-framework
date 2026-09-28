package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.Map;

/**
 * Magic-API 工具本地执行接口：function 按名调用，api 执行其脚本。
 *
 * <p>隔离 {@code ScriptManager} 静态依赖，使执行策略可替换、可测试。</p>
 */
public interface MagicToolInvoker {

	/**
	 * 调用 Magic-API 函数。
	 *
	 * @param name      函数名（如 {@code getEnv}）
	 * @param arguments 参数表
	 */
	Object invokeFunction(String name, Map<String, Object> arguments);

	/**
	 * 执行 Magic-API api 脚本。
	 *
	 * @param info      api 资源
	 * @param arguments 参数表
	 */
	Object invokeApi(ApiInfo info, Map<String, Object> arguments);
}
