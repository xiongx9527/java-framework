/**
 * agentic 静态工厂（AgenticServices）。
 *
 * <p>出处：
 * <ul>
 *   <li>langchain4j-agentic：{@code dev.langchain4j.agentic.AgenticServices}（静态 builder 方法；beta，Java 17）</li>
 *   <li>Spring AI：agentic（Observation / Graph）</li>
 *   <li>AgentScope：{@code AgenticScope} / {@code sequenceBuilder} / {@code loopBuilder} / {@code plannerBuilder} / {@code supervisorBuilder} / {@code humanInTheLoopBuilder} / {@code AgentListener} / {@code a2aBuilder}</li>
 * </ul>
 *
 * <p>本包位于 {@code fw-plus-ai-base}（所有 langchain4j 形状的抽象统一放这里），只提供官方形状的契约（静态 builder 方法 + 各 Service 接口）；具体实现由编排后端（{@code fw-plus-ai-agentscope-core} 等）提供。
 */
package com.xiongx9527.fw.plus.ai.agentic;
