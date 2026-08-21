package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 不依赖具体图框架的版本化 Agent 图。
 *
 * <p>图只描述稳定节点 ID 和依赖，不携带 Spring Bean、模型客户端、凭据或业务数据。构造后不可变，可在
 * 并发执行和检查点序列化中共享；版本发生变化时，恢复实现必须显式迁移或拒绝旧状态，禁止静默套用新图。</p>
 */
public final class AskDataAgentGraph {

	private final String id;
	private final String version;
	private final List<AskDataAgentNode> nodes;

	public AskDataAgentGraph(String id, String version, List<AskDataAgentNode> nodes) {
		if (id == null || id.trim().length() == 0 || version == null || version.trim().length() == 0
				|| nodes == null || nodes.isEmpty()) {
			throw new IllegalArgumentException("图定义不能为空");
		}
		this.id = id.trim();
		this.version = version.trim();
		this.nodes = Collections.unmodifiableList(new ArrayList<AskDataAgentNode>(nodes));
	}

	public String getId() { return id; }
	public String getVersion() { return version; }
	public List<AskDataAgentNode> getNodes() { return nodes; }
}
