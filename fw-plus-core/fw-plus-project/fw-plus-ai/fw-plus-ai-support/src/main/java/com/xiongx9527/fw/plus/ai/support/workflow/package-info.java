/**
 * 编排 / Agentic 端口（AgentScope 特殊情况的主要落点）。
 *
 * <p>出处：
 * <ul>
 *   <li>langchain4j：{@code langchain4j-agentic} 的 {@code sequenceBuilder} / {@code loopBuilder} / {@code plannerBuilder} / {@code supervisorBuilder}</li>
 *   <li>Spring AI：agentic（Observation / Graph 编排）</li>
 *   <li>AgentScope：{@code AgenticScope}（共享状态 writeState/readState）、{@code humanInTheLoopBuilder()}（人工介入）、
 *       {@code AgentListener}（10 个观测回调）、{@code a2aBuilder()}（跨进程）</li>
 * </ul>
 *
 * <p>端口只暴露 {@code goal} / {@code step} / {@code budget}，<b>不引入</b> Agent / Scope / Supervisor 概念——
 * 换掉 agentscope / langchain4j-agentic / spring-ai 任一实现都不必改端口（"能委托就委托"在编排层的同一条规则）。
 * 拓扑（顺序 / 并行 / 循环）由具体 backend 的 adapter 内部组装，端口不感知。
 */
package com.xiongx9527.fw.plus.ai.support.workflow;
