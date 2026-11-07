package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPrompt;

/**
 * SQL 生成能力（§6.38）。纯接口，与具体 LLM 框架无关；
 * 给定相同 {@code (question, schemaContext, dialectHint)} 结果可复现（确定性）。
 *
 * <p>两种用法共用同一实现：
 * <ul>
 *   <li>内联：问数 pipeline 的 GEN_SQL 步骤直接调用；</li>
 *   <li>工具：在 ToolPort 注册为 askdata.generate_sql，由 agent 自主决定调用。</li>
 * </ul>
 */
public interface SqlGenerationCapability {
    SqlGenResult generate(SqlGenInput input);

    /**
     * 流式生成（§6.38 流式输出）：返回底层 {@link LlmTokenStream}，调用方逐 token 消费。
     * 与 {@link #generate(SqlGenInput)} 共用同一套提示词构造，但不做抽取 / 校验 / 修复，仅透传原始流；
     * 调用方在收齐令牌后自行用 {@link SqlExtractor} 抽取 SQL / brief / chartType。
     */
    LlmTokenStream generateStream(SqlGenInput input);

    /**
     * 精炼模型原始输出（抽取 → 本地校验 → 修复）。供产品保留自有提示词组装 / 流式输出，
     * 同时复用框架通用 SQL 引擎（抽取 / 校验 / 修复轮）。
     *
     * <p>{@code prompt} 即产品已组装好的 system/user 提示词（修复轮会以其 user 提示词为基底回喂失败原因）；
     * {@code meta} 提供方言提示与修复策略，其 {@code schemaContext} 传 null/空表示跳过表可见性校验
     * （交由调用方自有逻辑约束，如产品的 {@code validateTables}）。本方法不做 prompt 组装、不要求 schemaContext 非空。</p>
     *
     * @throws SqlGenerationException 修复后仍不通过且 fail-closed 时抛出（与 {@link #generate} 一致）
     */
    SqlGenResult refine(String rawOutput, SqlPrompt prompt, SqlGenInput meta);
}
