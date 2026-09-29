package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.model.Langchain4jContentInjectorInfo;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-content-injector} 动态注册表：控制台新增/修改/删除资源即刷新注入器映射。
 */
public class Langchain4jContentInjectorMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jContentInjectorInfo> {

	public Langchain4jContentInjectorMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jContentInjectorInfo> magicResourceStorage) {
		super(magicResourceStorage);
	}

	@EventListener(condition = "#event.type == 'langchain4j-content-injector'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-content-injector'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}
}
