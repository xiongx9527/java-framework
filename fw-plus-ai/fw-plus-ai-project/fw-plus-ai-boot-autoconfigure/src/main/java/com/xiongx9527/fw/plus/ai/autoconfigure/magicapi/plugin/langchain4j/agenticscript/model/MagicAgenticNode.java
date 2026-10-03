package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.agenticscript.model;

import java.util.List;

import dev.langchain4j.agentic.UntypedAgent;

/**
 * agentic 脚本资源的节点形态契约：{@link MagicScriptAgent}（magic-script 节点）与
 * {@link MagicLlmAgent}（LLM 对话节点）的公共访问器——编排/图适配/工具包装按本接口分发，
 * 不感知具体节点实现。
 */
public interface MagicAgenticNode extends UntypedAgent {

	/** 资源路径（节点唯一标识） */
	String getPath();

	/** 节点名（缺省取 path） */
	String getName();

	/** 节点描述（供脚本与 LLM 理解用途） */
	String getDescription();

	/** 输入变量声明（对应编排的 inputKeys，invoke 入参按名注入） */
	List<String> getInputKeys();

	/** 输出 key：节点输出写回 AgenticScope 的变量名 */
	String getOutputKey();

	/** 资源形态（node / llm / tool / graph） */
	String getType();
}
