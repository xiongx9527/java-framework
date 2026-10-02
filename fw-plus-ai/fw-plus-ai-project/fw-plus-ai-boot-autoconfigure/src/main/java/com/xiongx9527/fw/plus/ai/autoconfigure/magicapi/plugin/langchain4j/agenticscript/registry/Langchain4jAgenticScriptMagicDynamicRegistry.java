package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.Langchain4jAgenticScriptInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-agentic-script} 动态注册器：资源型 .ms 脚本，控制台新增/修改/删除
 * 即刷新映射（与 system-message 同模式——脚本节点求值时实时读取，无需持有实例）。
 */
public class Langchain4jAgenticScriptMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jAgenticScriptInfo> {

	public Langchain4jAgenticScriptMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jAgenticScriptInfo> magicResourceStorage) {
		super(magicResourceStorage);
	}

	@EventListener(condition = "#event.type == 'langchain4j-agentic-script'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-agentic-script'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}
}
