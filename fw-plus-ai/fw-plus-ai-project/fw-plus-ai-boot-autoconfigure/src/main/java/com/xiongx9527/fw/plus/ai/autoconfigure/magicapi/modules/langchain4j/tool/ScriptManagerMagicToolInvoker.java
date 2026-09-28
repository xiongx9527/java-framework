package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool;

import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.magicapi.utils.ScriptManager;

import java.util.Map;

/**
 * 基于 Magic-API 脚本引擎的本地执行器：function 走 {@code magic.invoke(name, args)}，
 * api 直接执行其脚本并注入 args/params/body/apiInfo 变量。
 *
 * <p>执行路径与 MCP 插件的 {@code MagicApiMcpScriptInvoker} 一致，但不经过 MCP 协议层——
 * 工具调用就是本 JVM 内的一次脚本执行。</p>
 */
public class ScriptManagerMagicToolInvoker implements MagicToolInvoker {

	@Override
	public Object invokeFunction(String name, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("funcName", name);
		context.set("args", arguments);
		return ScriptManager.executeScript("import magic; return magic.invoke(funcName, args);", context);
	}

	@Override
	public Object invokeApi(ApiInfo info, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("args", arguments);
		context.set("params", arguments);
		context.set("body", arguments);
		context.set("apiInfo", info);
		return ScriptManager.executeScript(info.getScript(), context);
	}
}
