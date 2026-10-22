package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.module;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model.MagicDynamicLangchain4jSkillDataSource;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Skill 脚本模块：
 * {@code langchain4jSkill['key']} 取单个 {@link Skill}；
 * {@code langchain4jSkill.all()} 取全部技能聚合的 {@link Skills}（toolProvider / formatAvailableSkills）；
 * {@code langchain4jSkill.toolProvider()} 直接取全部技能聚合的 {@code ToolProvider}，
 * 挂到 {@code AssistantBuilder.toolProvider(...)} 或经 {@code AssistantBuilder.skills(...)} 注入。
 */
@MagicModule("langchain4jSkill")
public class Langchain4jSkillModule
		implements DynamicAttribute<Skill, Langchain4jSkillModule>, DynamicModule<Skill> {

	private final MagicDynamicLangchain4jSkillDataSource dataSource;

	public Langchain4jSkillModule(MagicDynamicLangchain4jSkillDataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Skill getDynamicAttribute(String key) {
		Skill skill = dataSource.get(key);
		if (skill == null) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.langchain4jModel.modelNotConfigured", "Skill", key, new TreeSet<>(dataSource.keys())));
		}
		return skill;
	}

	/** 全部已启用技能聚合的 {@link Skills}（每次调用按当前实例重建，热更新实时生效）。 */
	public Skills all() {
		return Skills.from(new ArrayList<>(dataSource.all().values()));
	}

	/** 全部已启用技能的副本列表（{@code AssistantBuilder.skills(...)} 数组/集合注入用）。 */
	public List<Skill> list() {
		return new ArrayList<>(dataSource.all().values());
	}

	/** 全部技能聚合的 {@code ToolProvider}（内含 activate_skill / read_skill_resource 工具）。 */
	public dev.langchain4j.service.tool.ToolProvider toolProvider() {
		return all().toolProvider();
	}

	/** 技能目录文本（XML 形式 {@code <available_skills>}），注入系统提示用。 */
	public String formatAvailableSkills() {
		return all().formatAvailableSkills();
	}

	@Override
	public Langchain4jSkillModule setDynamicAttribute(String key, Skill value) {
		throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
				"fw.ai.modelModule.scriptSetUnsupported", "langchain4j-skill"));
	}

	@Override
	public Skill getDynamicModule(MagicScriptContext context) {
		String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
		return getDynamicAttribute(key);
	}
}
