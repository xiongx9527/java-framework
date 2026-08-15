package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Agent 图中的不可变节点定义。
 *
 * <p>{@code id} 必须在同一图版本中稳定且唯一，依赖项只能引用同图节点。节点不保存执行器或用户上下文，
 * 避免图定义跨项目复用时泄露状态；图运行时必须检测未知依赖、循环依赖和重复执行。</p>
 */
public final class AskDataAgentNode {

	private final String id;
	private final List<String> dependsOn;

	public AskDataAgentNode(String id, List<String> dependsOn) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("id 不能为空");
		}
		this.id = id.trim();
		this.dependsOn = Collections.unmodifiableList(new ArrayList<String>(
				dependsOn == null ? Collections.<String>emptyList() : dependsOn));
	}

	public String getId() { return id; }
	public List<String> getDependsOn() { return dependsOn; }
}
