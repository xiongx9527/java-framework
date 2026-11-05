/**
 * 后端无关的 AI 基础能力契约。
 *
 * <p>本包只定义端口与数据模型，不绑定任何具体 LLM 框架（native / langchain4j / spring-ai /
 * agentscope 均由各自 adapter 落地）。编排与横切组合主要放在 {@code fw-plus-ai-support}，
 * 但 base 内也保留了少量可直接运行的默认实现与装饰器，便于零依赖使用。</p>
 *
 * <ul>
 *   <li>{@code model} —— 模型端口：大语言、向量、重排、内容审核、token 估算</li>
 *   <li>{@code store} —— 存储契约：{@code Store} / {@code RecordStore} 及向量、图存储端口</li>
 *   <li>{@code retrieval} —— 检索契约、单路实现与默认重排器</li>
 *   <li>{@code tool} —— 工具规格、执行器与调用环</li>
 *   <li>{@code service} —— 声明式 {@code AiServices}（注解接口 → 模型调用）</li>
 *   <li>{@code memory} / {@code template} / {@code output} —— 对话记忆、提示词模板、JSON 输出</li>
 *   <li>{@code event} / {@code trace} / {@code cost} / {@code cache} / {@code idempotency} /
 *       {@code guard} / {@code resilience} —— 监听、追踪与横切能力端口</li>
 *   <li>{@code intent} / {@code skill} / {@code io} / {@code mcp} —— 意图、技能、边界 IO、MCP 契约</li>
 *   <li>{@code context} —— 一次调用在整条链路里携带的身份与追踪信息</li>
 * </ul>
 *
 * <p><b>与 support 的分工</b>：base 保留少量可直接运行的默认实现与装饰器
 * （{@code DefaultRetrievalAugmentor} / {@code ToolCallingLoop} / {@code Listening*} / {@code AiServices}），
 * 便于零依赖使用；完整的多路检索管线、融合 / 重排策略、韧性 / 成本 / 缓存编排在
 * {@code fw-plus-ai-support}。两侧存在同名的
 * {@code RetrievalAugmentor} / {@code DefaultRetrievalAugmentor}，但签名与职责不同：
 * base 是「消息列表 + 查询 → 消息列表」的轻量挂钩，support 是「请求 → 结构化聚合结果」的编排入口
 * （support 侧已提供 {@code RagAugmentor} / {@code DefaultRagAugmentor} 作为推荐名字）。</p>
 *
 * <p>约束：本模块禁止 import langchain4j / spring-ai / agentscope 的具体类；目标 JDK 1.8。</p>
 */
package com.xiongx9527.fw.plus.ai.base;
