/**
 * AgentScope 实现核心：把编排契约映射到 AgentScope 运行时。
 *
 * <p><b>作用</b>：{@code fw-plus-ai-support} 只定义 {@code WorkflowPort} / {@code AgenticServices} 的中立契约，
 * 本模块负责用 AgentScope 的 Agent / AgenticScope / sequence·loop·planner·supervisor 编排器真正实现它们，
 * 并把 AgentScope 的观测（AgentListener）与人工介入（humanInTheLoop）桥接到框架的横切端口。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。对应 {@code io.agentscope.core.*}（agentscope-core 2.0.4）；
 * 契约形状对齐 {@code dev.langchain4j.agentic}（langchain4j-agentic 1.22.0-beta32）。</p>
 *
 * <p><b>运行约束</b>：Java 17 / Spring Boot 4 分支，默认关闭；Java 8 分支不装配本模块。</p>
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.agentscope.core;
