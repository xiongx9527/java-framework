package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.skill.model.Langchain4jSkillInfo.ResourceSpec;
import dev.langchain4j.skills.DefaultSkill;
import dev.langchain4j.skills.DefaultSkillResource;
import dev.langchain4j.skills.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code langchain4j-skill} 数据源的实例持有者：按 {@code key} 持有多个
 * {@link Skill} 实例。技能是纯内存对象（无连接/子进程资源），更新即重建。
 */
public class MagicDynamicLangchain4jSkillDataSource {

	private static final Logger logger = LoggerFactory.getLogger(MagicDynamicLangchain4jSkillDataSource.class);

	private final Map<String, Skill> skills = new ConcurrentHashMap<>();

	/** 按资源定义创建（或重建）一个 Skill 实例。 */
	public boolean create(Langchain4jSkillInfo info) {
		String key = info.getKey();
		if (key == null || key.trim().isEmpty()) {
			return false;
		}
		try {
			DefaultSkill.Builder builder = DefaultSkill.builder()
					.name(info.getName())
					.description(info.getDescription())
					.content(info.getContent() == null ? "" : info.getContent());
			if (info.getResources() != null && !info.getResources().isEmpty()) {
				List<dev.langchain4j.skills.SkillResource> resources = new ArrayList<>();
				for (ResourceSpec spec : info.getResources()) {
					resources.add(DefaultSkillResource.builder()
							.relativePath(spec.getPath())
							.content(spec.getContent() == null ? "" : spec.getContent())
							.build());
				}
				builder.resources(resources);
			}
			skills.put(key, builder.build());
			logger.info("注册 LangChain4j Skill: {} -> {}", key, info.getName());
			return true;
		} catch (Throwable e) {
			logger.warn("构建 LangChain4j Skill 失败: key={}", key, e);
			return false;
		}
	}

	/** 按 key 注销实例。 */
	public void destroy(String key) {
		if (key != null) {
			skills.remove(key);
		}
	}

	/** 按 key 取 Skill，不存在时返回 {@code null}。 */
	public Skill get(String key) {
		return key == null ? null : skills.get(key);
	}

	/** 当前已配置的 key 集合（不可修改视图）。 */
	public Set<String> keys() {
		return Collections.unmodifiableSet(skills.keySet());
	}

	/** 全部实例（key → Skill），聚合 Skills 用。 */
	public Map<String, Skill> all() {
		return Collections.unmodifiableMap(skills);
	}
}
