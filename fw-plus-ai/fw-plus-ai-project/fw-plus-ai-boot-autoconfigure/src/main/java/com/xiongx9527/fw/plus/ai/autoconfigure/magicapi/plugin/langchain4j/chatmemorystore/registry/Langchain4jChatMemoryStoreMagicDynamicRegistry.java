package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.Langchain4jChatMemoryStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.MagicDynamicLangchain4jChatMemoryStoreDataSource;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-chat-memory-store} 动态注册器：控制台新增/修改/删除资源即增删 ChatMemoryStore 实例。
 */
public class Langchain4jChatMemoryStoreMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jChatMemoryStoreInfo> {

	private final MagicDynamicLangchain4jChatMemoryStoreDataSource dataSource;

	/** Spring 容器中的默认 ChatMemoryStore（langchain4j 自动配置注入），可能不存在 */
	private final ObjectProvider<ChatMemoryStore> defaultInstance;

	public Langchain4jChatMemoryStoreMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jChatMemoryStoreInfo> magicResourceStorage,
			MagicDynamicLangchain4jChatMemoryStoreDataSource dataSource,
			ObjectProvider<ChatMemoryStore> defaultInstance) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
		this.defaultInstance = defaultInstance;
	}

	/** 默认 ChatMemoryStore（Spring 注入，key=default，只读展示）+ 控制台配置的数据源，一并暴露 */
	@Override
	public List<Langchain4jChatMemoryStoreInfo> defaultMappings() {
		List<Langchain4jChatMemoryStoreInfo> result = new ArrayList<>();
		ChatMemoryStore springDefault = defaultInstance == null ? null : defaultInstance.getIfAvailable();
		if (springDefault != null) {
			Langchain4jChatMemoryStoreInfo info = new Langchain4jChatMemoryStoreInfo();
			info.setKey("default");
			info.setName("默认数据源");
			info.setEnabled(true);
			result.add(info);
		}
		result.addAll(mappings());
		return result;
	}

	@EventListener(condition = "#event.type == 'langchain4j-chat-memory-store'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-chat-memory-store'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<Langchain4jChatMemoryStoreInfo> mappingNode) {
		Langchain4jChatMemoryStoreInfo entity = mappingNode.getEntity();
		dataSource.destroy(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.create(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<Langchain4jChatMemoryStoreInfo> mappingNode) {
		dataSource.destroy(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}
}
