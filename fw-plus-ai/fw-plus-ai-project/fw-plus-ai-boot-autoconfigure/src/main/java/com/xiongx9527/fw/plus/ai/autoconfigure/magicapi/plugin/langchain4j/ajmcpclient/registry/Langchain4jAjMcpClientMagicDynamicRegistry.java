package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.model.Langchain4jAjMcpClientInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.ajmcpclient.model.MagicDynamicLangchain4jAjMcpClientDataSource;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-aj-mcp-client} 动态注册器：控制台新增/修改/删除资源即新建/重建/关闭
 * 对应的 aj-mcp 客户端。
 */
public class Langchain4jAjMcpClientMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jAjMcpClientInfo> {

	private final MagicDynamicLangchain4jAjMcpClientDataSource dataSource;

	public Langchain4jAjMcpClientMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jAjMcpClientInfo> magicResourceStorage,
			MagicDynamicLangchain4jAjMcpClientDataSource dataSource) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
	}

	@EventListener(condition = "#event.type == 'langchain4j-aj-mcp-client'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-aj-mcp-client'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<Langchain4jAjMcpClientInfo> mappingNode) {
		Langchain4jAjMcpClientInfo entity = mappingNode.getEntity();
		dataSource.destroy(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.create(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<Langchain4jAjMcpClientInfo> mappingNode) {
		dataSource.destroy(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}
}
