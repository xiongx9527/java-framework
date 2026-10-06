package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.magicapi.utils.ScriptManager;

import java.util.Map;

/**
 * 通过 Magic-API 脚本引擎执行工具：function 走 {@code magic.invoke(name, args)}，
 * api 直接执行其脚本并注入 args/params/body 变量。
 */
public class MagicApiMcpScriptInvoker implements MagicApiMcpToolInvoker.FunctionInvoker, MagicApiMcpToolInvoker.ApiInvoker {

	@Override
	public Object invoke(MagicApiMcpTool tool, Map<String, Object> arguments) {
		return invokeFunction(tool.getName(), arguments);
	}

	public Object invokeFunction(String functionName, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("funcName", functionName);
		context.set("args", arguments);
		return ScriptManager.executeScript("import magic; return magic.invoke(funcName, args);", context);
	}

	@Override
	public Object invoke(ApiInfo info, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("args", arguments);
		context.set("params", arguments);
		context.set("body", arguments);
		context.set("apiInfo", info);
		return ScriptManager.executeScript(info.getScript(), context);
	}
}
