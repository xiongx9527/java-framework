/**
 * 技能（Skill）抽象：{@code SKILL.md} + 资源包 的装载、注册、筛选与提示词渲染。
 *
 * <p>整套抽象属于 B 类出处（AgentScope 特殊抽象），镜像 {@code io.agentscope.core.skill}
 * （agentscope-core 2.0.4），语义遵循 Anthropic Agent Skills 的 {@code SKILL.md} 约定；
 * langchain4j 1.23.0 与 Spring AI 均无对应类型，因此这里只定义端口，具体实现由
 * {@code fw-plus-ai-agentscope} 提供，其它适配器可按同一端口自行实现。</p>
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.skill;
