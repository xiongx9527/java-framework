package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model.Langchain4jSkillInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model.MagicDynamicLangchain4jSkillDataSource;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * {@code langchain4j-skill} 动态注册器：控制台新增/修改/删除资源即重建/注销对应 Skill 实例。
 */
public class Langchain4jSkillMagicDynamicRegistry
		extends AbstractMagicDynamicRegistry<Langchain4jSkillInfo> {

	private final MagicDynamicLangchain4jSkillDataSource dataSource;

	public Langchain4jSkillMagicDynamicRegistry(
			MagicResourceStorage<Langchain4jSkillInfo> magicResourceStorage,
			MagicDynamicLangchain4jSkillDataSource dataSource) {
		super(magicResourceStorage);
		this.dataSource = dataSource;
	}

	@EventListener(condition = "#event.type == 'langchain4j-skill'")
	public void onFileEvent(FileEvent event) {
		processEvent(event);
	}

	@EventListener(condition = "#event.type == 'langchain4j-skill'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}

	@Override
	protected boolean register(MappingNode<Langchain4jSkillInfo> mappingNode) {
		Langchain4jSkillInfo entity = mappingNode.getEntity();
		dataSource.destroy(entity.getKey());
		if (!entity.isEnabled()) {
			return true;
		}
		boolean created = dataSource.create(entity);
		return created && super.register(mappingNode);
	}

	@Override
	protected void unregister(MappingNode<Langchain4jSkillInfo> mappingNode) {
		dataSource.destroy(mappingNode.getEntity().getKey());
		super.unregister(mappingNode);
	}
}
