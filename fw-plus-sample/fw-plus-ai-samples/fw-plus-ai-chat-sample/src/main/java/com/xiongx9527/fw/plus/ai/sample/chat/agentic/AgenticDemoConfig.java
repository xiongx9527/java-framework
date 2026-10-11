package com.xiongx9527.fw.plus.ai.sample.chat.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticPipelineRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticSupervisor;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.AgenticToolProviders;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.tool.ToolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agentic 智能体编排的 Spring 装配：注册中心 + 编排 Bean + agent-as-tool。
 *
 * <p>设计要点：</p>
 * <ul>
 *     <li>编排「构建」与「执行」分离——Bean 装配阶段只构建编排（不触达模型与网络），
 *         只有 REST 端点被调用时才 {@code invoke(...)} 真正请求大模型。</li>
 *     <li>{@code item-map-pipeline} 是非 AI 型的 {@code parallelMapper} 编排（无需模型），
 *         注册进 {@link AgenticPipelineRegistry} 后由 key 复用；它同时被包装为
 *         {@link ToolProvider}（agent-as-tool），可挂到任何助手/agent 的工具位。</li>
 *     <li>{@code supervisor} 是 LLM 自主编排：监督者依据请求自主挑选并调用 subAgents，
 *         产物为 {@link AgenticSupervisor} 句柄，{@code invoke(String)} 单入口执行。</li>
 * </ul>
 */
@Configuration
public class AgenticDemoConfig {

	private static final Logger log = LoggerFactory.getLogger(AgenticDemoConfig.class);

	/** 编排注册中心：进程内 Map，注册的编排可经 key 复用（含包装为工具）。 */
	@Bean
	public AgenticPipelineRegistry agenticPipelineRegistry() {
		return new AgenticPipelineRegistry();
	}

	/**
	 * 演示编排：parallelMapper（列表逐项并行映射，非 AI，无需模型即可跑通）。
	 *
	 * <p>注册到 {@link AgenticPipelineRegistry}，key = {@code item-map}；REST 端点和
	 * {@code /api/chat/agentic/map} 均按 key 取用，体现「编排一次注册、多处复用」。</p>
	 */
	@Bean
	public UntypedAgent itemMapPipeline(AgenticPipelineRegistry registry) {
		UntypedAgent pipeline = AgenticWorkflowsDemo.parallelMapperDemo();
		registry.register("item-map", pipeline);
		log.info("已注册 agentic 编排 [item-map]（parallelMapper，无需模型）");
		return pipeline;
	}

	/**
	 * LLM 自主编排：supervisor（监督者）依据请求自主选择并调用 subAgents。
	 *
	 * <p>仅构建、不执行：Bean 装配阶段不触达模型与网络，构造失败只发生在
	 * {@code AgenticController} 的 {@code /api/chat/agentic/supervisor} 端点被调用时
	 * （此时需要 Ollama 可用，否则模型请求报错）。</p>
	 */
	@Bean
	public AgenticSupervisor supervisor(ChatModel chatModel) {
		AgenticSupervisor supervisor = AgenticWorkflowsDemo.supervisorDemo(
				chatModel, AgenticWorkflowsDemo.observabilityDemo());
		log.info("已装配 supervisor 编排（构建完成，invoke 时才请求模型；已挂 AgentListener）");
		return supervisor;
	}

	/**
	 * agent-as-tool：把注册中心中的编排包装为 {@link ToolProvider}。
	 *
	 * <p>包装后编排可以像普通工具一样挂到助手的 {@code toolProvider} 位（见
	 * {@code AssistantBuilder.toolProvider(...)}），供大模型自主决定何时调用整条流水线。</p>
	 */
	@Bean
	public ToolProvider agenticToolProvider(AgenticPipelineRegistry registry, UntypedAgent itemMapPipeline) {
		return AgenticToolProviders.agenticToolProviderByKey(
				registry, "item-map",
				"map_items", "对给定列表逐项并行映射（整条 agentic 流水线），返回映射后的列表",
				"results", "items");
	}
}