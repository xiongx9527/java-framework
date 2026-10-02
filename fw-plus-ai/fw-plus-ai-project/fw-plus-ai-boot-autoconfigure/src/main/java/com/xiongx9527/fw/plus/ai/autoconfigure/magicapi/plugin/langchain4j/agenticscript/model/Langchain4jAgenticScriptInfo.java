package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

import java.util.List;

/**
 * {@code langchain4j-agentic-script} 资源定义：一段 magic-script 实现的 agentic 编排节点
 * 或编排工具（用 {@code type} 字段区分形态）。
 *
 * <p>继承 {@link PathMagicEntity}：{@code path} 为资源路径，{@code script} 为脚本正文
 * （magic-script，输入变量已注入脚本上下文，返回值即节点输出/工具返回值）。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jAgenticScriptInfo extends PathMagicEntity {

	/** 形态：node = 编排节点（挂入 AgenticWorkflows 编排 subAgents）；tool = 编排工具（包装为 ToolProvider） */
	private String type;

	/** 实例唯一 key，脚本按 key 取节点/工具 */
	private String key;

	/** 展示名，缺省取 key */
	private String name;

	/** 描述（作为编排节点/工具的说明，供脚本与 LLM 理解用途） */
	private String description;

	/** 输入变量声明（入参名列表，对应编排的 inputKeys，求值时注入脚本上下文） */
	private List<String> inputKeys;

	/** 输出 key：节点输出写回 AgenticScope 的变量名（type=node 时使用，可选） */
	private String outputKey;

	/** 是否启用；停用时注销实例（下线而非删除） */
	private boolean enabled = true;

	@Override
	public Langchain4jAgenticScriptInfo simple() {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		super.simple(info);
		info.setType(this.type);
		info.setKey(this.key);
		info.setName(this.name);
		info.setDescription(this.description);
		info.setInputKeys(this.inputKeys);
		info.setOutputKey(this.outputKey);
		info.setEnabled(this.enabled);
		return info;
	}

	@Override
	public Langchain4jAgenticScriptInfo copy() {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		super.copyTo(info);
		info.setType(this.type);
		info.setKey(this.key);
		info.setName(this.name);
		info.setDescription(this.description);
		info.setInputKeys(this.inputKeys);
		info.setOutputKey(this.outputKey);
		info.setEnabled(this.enabled);
		return info;
	}
}
