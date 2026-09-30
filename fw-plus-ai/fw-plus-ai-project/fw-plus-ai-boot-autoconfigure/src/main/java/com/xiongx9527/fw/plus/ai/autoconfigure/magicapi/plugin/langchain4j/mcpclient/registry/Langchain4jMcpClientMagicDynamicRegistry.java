package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.Langchain4jMcpClientInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.mcpclient.model.MagicDynamicLangchain4jMcpClientDataSource;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-mcp-client} 动态注册器：控制台新增/修改/删除资源即新建/重建/关闭
 * 对应的 MCP 客户端。
 */
public class Langchain4jMcpClientMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jMcpClientInfo> {

	private final MagicDynamicLangchain4jMcpClientDataSource dataSource;

	public Langchain4jMcpClientMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jMcpClientInfo> magicResourceStorage,
			MagicDynamicLangchain4jMcpClientDataSource dataSource) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
	}

	@EventListener(condition = "#event.type == 'langchain4j-mcp-client'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-mcp-client'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<Langchain4jMcpClientInfo> mappingNode) {
		Langchain4jMcpClientInfo entity = mappingNode.getEntity();
		dataSource.destroy(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.create(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<Langchain4jMcpClientInfo> mappingNode) {
		dataSource.destroy(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}
}
