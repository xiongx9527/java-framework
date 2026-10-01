package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.List;

/**
 * {@code langchain4j-skill} 数据源资源定义：声明一个 Agent Skill（可复用、自包含的
 * 行为指令包），脚本用 {@code langchain4jSkill['key']} 取用，或经
 * {@code AssistantBuilder.skills(...)} 聚合注入。
 *
 * <p>技能形态遵循 Agent Skills 规范：name + description 组成目录（初始上下文只放这两项），
 * content 为激活后载入的完整指令，resources 为激活后可按需读取的参考文档。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jSkillInfo extends MagicEntity {

	/** 实例唯一 key，脚本按 key 取 Skill */
	private String key;

	/** 技能名（Agent Skills 规范的 name，如 "docx"），即 activate_skill 的入参 */
	private String name;

	/** 技能描述（Agent Skills 规范的 description）：进入初始上下文的目录条目，LLM 据此决定是否激活 */
	private String description;

	/** 指令正文（SKILL.md 的 body）：activate_skill 激活后载入上下文的完整内容 */
	private String content;

	/** 激活后可按需读取的参考文档（read_skill_resource 按 relativePath 读取） */
	private List<ResourceSpec> resources;

	/** 是否启用；停用时注销实例（下线而非删除） */
	private boolean enabled = true;

	/** 内嵌参考文档：相对路径 + 全文（内容式，不做文件系统挂载） */
	@Data
	public static class ResourceSpec {

		/** 相对路径（如 references/tone-guide.md），read_skill_resource 的入参 */
		private String path;

		/** 文档全文 */
		private String content;
	}
}
