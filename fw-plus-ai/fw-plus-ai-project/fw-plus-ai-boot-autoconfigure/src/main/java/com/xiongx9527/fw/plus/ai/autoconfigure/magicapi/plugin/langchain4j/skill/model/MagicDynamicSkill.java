package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.module.Langchain4jSkillModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.SkillResource;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.Collections;
import java.util.List;

/**
 * {@link Skill} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前实例，
 * magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的 Skill bean
 * （key 空取默认、非空按 bean 名），与 {@code MagicDynamicChatModel} 的兜底语义一致。
 */
public class MagicDynamicSkill implements Skill {

	private final Langchain4jSkillModule skillModule;

	private final String key;

	public MagicDynamicSkill(Langchain4jSkillModule skillModule, String key) {
		this.skillModule = skillModule;
		this.key = key;
	}

	private Skill getSkill() {
		try {
			return skillModule.getDynamicAttribute(key);
		} catch (Exception e) {
			if (StringUtils.isEmpty(key)) {
				return SpringUtils.getBean(Skill.class);
			}
			return SpringUtils.getBean(key, Skill.class);
		}
	}

	@Override
	public String name() {
		return getSkill().name();
	}

	@Override
	public String description() {
		return getSkill().description();
	}

	@Override
	public String content() {
		return getSkill().content();
	}

	@Override
	public List<SkillResource> resources() {
		List<SkillResource> resources = getSkill().resources();
		return resources == null ? Collections.emptyList() : resources;
	}

	@Override
	public List<ToolProvider> toolProviders() {
		List<ToolProvider> toolProviders = getSkill().toolProviders();
		return toolProviders == null ? Collections.emptyList() : toolProviders;
	}
}
