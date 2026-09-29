package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.model.Langchain4jSystemMessageInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-system-message} 动态注册表：控制台新增/修改/删除资源即增删系统提示词映射。
 */
public class Langchain4jSystemMessageMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jSystemMessageInfo> {

	public Langchain4jSystemMessageMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jSystemMessageInfo> magicResourceStorage) {
		super(magicResourceStorage);
	}

	@EventListener(condition = "#event.type == 'langchain4j-system-message'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-system-message'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}
}
