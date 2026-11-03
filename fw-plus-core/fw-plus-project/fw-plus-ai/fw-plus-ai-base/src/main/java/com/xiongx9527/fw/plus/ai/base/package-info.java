/**
 * 后端无关的 AI 基础能力契约。
 *
 * <p>本包只定义端口与数据模型，不绑定任何具体 LLM 框架（native / langchain4j / spring-ai /
 * agentscope 均由各自 adapter 落地），也不包含编排与横切逻辑（那部分在
 * {@code fw-plus-ai-support}）。</p>
 *
 * <ul>
 *   <li>{@code model} —— 模型端口：大语言、向量、重排</li>
 *   <li>{@code store} —— 向量存储与检索：向量检索、全文检索</li>
 * </ul>
 *
 * <p>约束：本模块禁止 import langchain4j / spring-ai / agentscope 的具体类。</p>
 */
package com.xiongx9527.fw.plus.ai.base;
