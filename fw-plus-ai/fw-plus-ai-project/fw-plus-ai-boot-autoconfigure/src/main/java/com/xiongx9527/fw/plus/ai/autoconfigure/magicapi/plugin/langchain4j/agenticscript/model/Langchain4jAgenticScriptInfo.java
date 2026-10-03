package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

import java.util.List;

/**
 * {@code langchain4j-agentic-script} 资源定义：一段 magic-script 实现的 agentic 编排节点
 * 或编排工具（用 {@code type} 字段区分形态）。
 *
 * <p>继承 {@link PathMagicEntity}：{@code path} 为资源路径（即节点/工具唯一标识），
 * {@code script} 为脚本正文（magic-script，输入变量已注入脚本上下文，返回值即节点输出/工具返回值）。</p>
 *
 * <p>{@code type=llm} 时为 LLM 节点形态：{@code script} 正文是 user message 模板
 * （{@code {{var}}} 按输入变量插值），{@code systemMessage} 为系统提示，{@code modelKey}
 * 指定 {@code langchain4jChatModel} 资源 key；调用即一次对话补全，返回模型文本。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jAgenticScriptInfo extends PathMagicEntity {

	/** 形态：node = 编排节点（挂入 AgenticWorkflows 编排 subAgents，或经 MagicScriptGraphNodes 嵌入 langgraph4j 图）；
	 * tool = 编排工具（包装为 ToolProvider）；
	 * graph = 图编排脚本（脚本构建并返回 langgraph4j StateGraph，可编译执行或包装为 ToolProvider）；
	 * llm = LLM 节点（script 为 user message 模板，调用一次对话补全） */
	private String type;

	/** 展示名，缺省取 path */
	private String name;

	/** 描述（作为编排节点/工具的说明，供脚本与 LLM 理解用途） */
	private String description;

	/** 输入变量声明（入参名列表，对应编排的 inputKeys，求值时注入脚本上下文） */
	private List<String> inputKeys;

	/** 输出 key：节点输出写回 AgenticScope 的变量名（type=node 时使用，可选；嵌入图时作为 state 键，缺省 output） */
	private String outputKey;

	/** 测试参数（JSON 对象字符串）：node/tool 求值时注入脚本上下文；graph 作为图的执行输入 */
	private String params;

	/** LLM 节点的模型 key（type=llm 时使用）：对应 {@code langchain4jChatModel['<modelKey>']}；解析失败回落容器 ChatModel bean */
	private String modelKey;

	/** LLM 节点的系统提示（type=llm 时使用，可选） */
	private String systemMessage;

	@Override
	public Langchain4jAgenticScriptInfo simple() {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		super.simple(info);
		info.setType(this.type);
		info.setName(this.name);
		info.setDescription(this.description);
		info.setInputKeys(this.inputKeys);
		info.setOutputKey(this.outputKey);
		info.setParams(this.params);
		info.setModelKey(this.modelKey);
		info.setSystemMessage(this.systemMessage);
		return info;
	}

	@Override
	public Langchain4jAgenticScriptInfo copy() {
		Langchain4jAgenticScriptInfo info = new Langchain4jAgenticScriptInfo();
		super.copyTo(info);
		info.setType(this.type);
		info.setName(this.name);
		info.setDescription(this.description);
		info.setInputKeys(this.inputKeys);
		info.setOutputKey(this.outputKey);
		info.setParams(this.params);
		info.setModelKey(this.modelKey);
		info.setSystemMessage(this.systemMessage);
		return info;
	}
}
