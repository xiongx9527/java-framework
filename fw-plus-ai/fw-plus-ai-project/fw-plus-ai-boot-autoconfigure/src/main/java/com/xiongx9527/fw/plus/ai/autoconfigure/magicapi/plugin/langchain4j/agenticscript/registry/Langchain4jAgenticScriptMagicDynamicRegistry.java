package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.Langchain4jAgenticScriptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model.MagicDynamicLangchain4jAgenticScriptDataSource;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-agentic-script} 动态注册器：控制台新增/修改/删除资源即重建/注销脚本节点。
 */
public class Langchain4jAgenticScriptMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jAgenticScriptInfo> {

	private final MagicDynamicLangchain4jAgenticScriptDataSource dataSource;

	public Langchain4jAgenticScriptMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jAgenticScriptInfo> magicResourceStorage,
			MagicDynamicLangchain4jAgenticScriptDataSource dataSource) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
	}

	@EventListener(condition = "#event.type == 'langchain4j-agentic-script'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-agentic-script'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<Langchain4jAgenticScriptInfo> mappingNode) {
		Langchain4jAgenticScriptInfo entity = mappingNode.getEntity();
		dataSource.destroy(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.create(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<Langchain4jAgenticScriptInfo> mappingNode) {
		dataSource.destroy(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}
}
