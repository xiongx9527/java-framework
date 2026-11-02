/**
 * fw-plus-ai 端口层（纽带）：框架无关的 AI 抽象。
 *
 * <p><b>抽象出处原则</b>：以 <b>langchain4j 1.23.0</b>（最新版）的核心 API 作为主抽象来源（模型 / 嵌入 / 检索 / 消息 / 工具），
 * 同时对照 <b>Spring AI</b> 的等价类型（ChatModel / StreamingChatModel / EmbeddingModel / VectorStore / SearchRequest / TokenCountEstimator），
 * 并吸收 <b>AgentScope</b> 在编排层的特殊情况（AgenticScope 共享状态、humanInTheLoop 人工介入、AgentListener 观测、a2a 跨进程）。
 *
 * <p><b>分层</b>：<b>所有 langchain4j 形状的抽象一律在本模块</b>——AI 组件端口（model / store / retriever / message / tool / template / mcp /
 * service / skill）与编排契约（agentic：AgenticServices / AgentBuilder / 各编排 Service），前者由各后端的组件子模块实现、
 * 后者由编排后端（{@code fw-plus-ai-agentscope} 等）实现。
 * <p>非 langchain4j 来源的增强抽象（workflow / context / resilience / cost / cache / idempotency / guard / io / 远程 skill）在
 * {@code fw-plus-ai-support}（依赖本模块，由各后端 adapter 实现）。
 *
 * <p><b>铁律</b>：本包及其子包禁止 import {@code dev.langchain4j}、{@code org.springframework.ai}、
 * {@code io.agentscope} 等任何具体框架类。所有类型均为自有中立 API，后端适配在各 {@code fw-plus-ai-*} 模块实现。
 */
package com.xiongx9527.fw.plus.ai;
