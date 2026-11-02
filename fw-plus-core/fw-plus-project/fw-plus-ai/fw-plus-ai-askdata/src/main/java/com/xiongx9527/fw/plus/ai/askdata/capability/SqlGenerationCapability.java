package com.xiongx9527.fw.plus.ai.askdata.capability;

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
}
