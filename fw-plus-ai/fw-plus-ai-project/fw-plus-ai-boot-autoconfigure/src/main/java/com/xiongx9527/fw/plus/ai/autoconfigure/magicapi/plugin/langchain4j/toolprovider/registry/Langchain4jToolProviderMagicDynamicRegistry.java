package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.Langchain4jToolProviderInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider.model.MagicDynamicLangchain4jToolProviderDataSource;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-tool-provider} 动态注册器：控制台新增/修改/删除资源即新增/更新/移除 ToolProvider 实例。
 */
public class Langchain4jToolProviderMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jToolProviderInfo> {

	private final MagicDynamicLangchain4jToolProviderDataSource dataSource;

	public Langchain4jToolProviderMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jToolProviderInfo> magicResourceStorage,
			MagicDynamicLangchain4jToolProviderDataSource dataSource) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
	}

	@EventListener(condition = "#event.type == 'langchain4j-tool-provider'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-tool-provider'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<Langchain4jToolProviderInfo> mappingNode) {
		Langchain4jToolProviderInfo entity = mappingNode.getEntity();
		dataSource.destroy(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.create(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<Langchain4jToolProviderInfo> mappingNode) {
		dataSource.destroy(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}
}
