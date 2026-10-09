package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * {@code Assistant} 的声明式接口：一次对话、一次流式输出，消息体为多模态内容列表。
 *
 * <p>两条链路共用同一个 Assistant 与同一套装配（模型 + 工具 + RAG），保证行为一致。
 * 实例由 {@link AssistantBuilder} 产出——其内部是 {@link MultimodalAssistantBridge}
 * 双层结构，<b>不要</b>用 {@code AiServices.builder(Assistant.class)} 裸建：那样会绕过
 * 桥接，多模态内容会被 {@code toString()} 成地址串（原生管线只认纯文本）。</p>
 *
 * <p>模型类型是 langchain4j 1.x 的 {@link dev.langchain4j.model.chat.ChatModel} 与
 * {@link dev.langchain4j.model.chat.StreamingChatModel}（springboot3 分支 1.20.2 起生效）。</p>
 *
 * <p>调用形态按 contents 分两条路径：</p>
 * <ul>
 *   <li><b>纯文本轮</b>（全部为 {@link dev.langchain4j.data.message.TextContent}）：
 *       拆出文本转发内层 {@link TextAssistant}，走 LangChain4j 原生完整管线——
 *       {@code {{context}}} 模板渲染、工具循环（含缺参保护）、RAG、会话记忆；</li>
 *   <li><b>多模态轮</b>（含图片/文档等）：自拼 {@code UserMessage} 直调模型，
 *       RAG 照走（query 取文本部分），会话记忆写<b>文本占位</b>
 *       （{@code [图片]} / {@code [文档:x.pdf]} + 文本，不存原文），
 *       本轮不进工具循环。</li>
 * </ul>
 *
 * <p>{@code context} 参数类型为 {@link Object}，只接受两种形态：{@code String} 当一句话背景
 * 原样透传；{@code Map} 经 fastjson2 序列化为 JSON；{@code null} 视为无背景。
 * 其他类型抛 {@link IllegalArgumentException}。两种形态最终统一为一段文本——
 * 纯文本轮随 {@code {{context}}} 占位语义进入（消息无占位则忽略，与历史行为一致），
 * 多模态轮以 TextContent 附加在消息头部（本轮生效，不入记忆）。</p>
 *
 * <p>多模态 {@link Content} 位于 {@code dev.langchain4j.data.message} 包（本仓库自维护的
 * langchain4j 1.20.2 fork 保留了该包结构，未随上游 1.x 挪到 {@code data.content}）。</p>
 *
 * <p>第一个参数带 {@link MemoryId}：<b>不可传 null</b>——裸 {@code AiServices} 代理会直接抛
 * {@code IllegalArgumentException}，由调用方兜底为 {@code default}。每个参数都必须带注解
 * （{@code @V} / {@code @UserMessage} / {@code @MemoryId}），裸参数在裸代理下调用时抛
 * IllegalConfigurationException。</p>
 *
 * <p>系统提示（人设 + profile）走 {@code systemMessageProvider}，由 {@link SystemPromptBuilder}
 * 拼装，按 memoryId 定制；检索资料靠 query，provider 入参只有 memoryId。</p>
 */
public interface Assistant {

	/**
	 * 同步对话：返回完整回复。
	 *
	 * @param memoryId 会话标识，同时是会话记忆与系统提示 provider 的键
	 * @param context  上下文：{@code String}（一句话背景）或 {@code Map}（结构化数据组，序列化为 JSON），可为 null
	 * @param contents 用户消息内容列表（文本 / 图片 / 文档等，不可为空）
	 * @return 回复文本
	 * @throws IllegalArgumentException contents 为空，或 context 为既非 String 也非 Map 的类型
	 * @throws IllegalStateException     多模态轮未配置对应模型（chatModel / streamingModel）
	 */
	String chat(@MemoryId String memoryId, @V("context") Object context, @UserMessage List<Content> contents);

	/**
	 * 流式对话：逐 token 回调。
	 *
	 * @param memoryId 会话标识，同时是会话记忆与系统提示 provider 的键
	 * @param context  上下文：{@code String}（一句话背景）或 {@code Map}（结构化数据组，序列化为 JSON），可为 null
	 * @param contents 用户消息内容列表（文本 / 图片 / 文档等，不可为空）
	 * @return 可挂 {@code onNext/onRetrieved/onComplete/onError} 的 {@link TokenStream}
	 * @throws IllegalArgumentException contents 为空，或 context 为既非 String 也非 Map 的类型
	 * @throws IllegalStateException     多模态轮未配置 streamingModel
	 */
	TokenStream stream(@MemoryId String memoryId, @V("context") Object context, @UserMessage List<Content> contents);
}
