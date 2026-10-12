package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.AbstractLangchain4jPluginConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model.MagicDynamicLangchain4jSkillDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.module.Langchain4jSkillModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.registry.Langchain4jSkillMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.resource.Langchain4jSkillMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j-skill} 数据源的装配：实例持有者、资源存储、动态注册器与脚本模块
 * {@code langchain4jSkill['key']} / {@code langchain4jSkill.all()}。
 *
 * <p>把 Agent Skills（Agent Skills 规范，langchain4j-skills 构件）暴露给脚本与
 * {@code AssistantBuilder.skills(...)}：初始上下文只放技能目录（name/description），
 * LLM 按需 {@code activate_skill} 取完整指令、{@code read_skill_resource} 读参考文档。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.skills.Skills")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.skill", name = "enabled",
		havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jSkillConfiguration extends AbstractLangchain4jPluginConfiguration {

	private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-skill.1.0.0.iife.js";

	public Langchain4jSkillConfiguration() {
		super("langchain4j-skill", "MagicLangchain4jSkill", JAVASCRIPT_FILENAME, false);
	}

	/** 数据源实例持有者：按 key 持有多个 Skill 实例。 */
	@Bean
	@ConditionalOnMissingBean
	public MagicDynamicLangchain4jSkillDataSource langchain4jSkillDataSource() {
		return new MagicDynamicLangchain4jSkillDataSource();
	}

	/** langchain4j-skill 资源类型存储。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jSkillMagicResourceStorage langchain4jSkillMagicResourceStorage() {
		return new Langchain4jSkillMagicResourceStorage();
	}

	/** langchain4j-skill 动态注册器：资源变更即重建/注销 Skill。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jSkillMagicDynamicRegistry langchain4jSkillMagicDynamicRegistry(
			Langchain4jSkillMagicResourceStorage storage,
			MagicDynamicLangchain4jSkillDataSource dataSource) {
		return new Langchain4jSkillMagicDynamicRegistry(storage, dataSource);
	}

	/** 脚本模块：langchain4jSkill['key'] 取单个 Skill，all()/toolProvider() 取聚合。 */
	@Bean
	@ConditionalOnMissingBean
	public Langchain4jSkillModule langchain4jSkillModule(
			MagicDynamicLangchain4jSkillDataSource dataSource) {
		return new Langchain4jSkillModule(dataSource);
	}
}
