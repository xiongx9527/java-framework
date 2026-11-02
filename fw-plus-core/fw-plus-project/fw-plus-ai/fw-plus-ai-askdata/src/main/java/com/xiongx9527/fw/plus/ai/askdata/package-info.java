/**
 * fw-plus-ai-askdata — 数据问答（askdata）通用领域封装（DomainPack）。
 *
 * <p>参考 docs/ai-layer：把问数链路上与后端无关的“生成 / 执行 / 可视化”三段能力提成
 * 一等、可独立调用的能力，供内联问数 pipeline 与工具调用（MCP / function calling）共用。</p>
 *
 * <p>已封装能力：
 * <ul>
 *   <li>{@code capability.SqlGenerationCapability}（§6.38）：NL2SQL 纯函数 + 显式输入，
 *       本地规则校验 + LLM 修复环；</li>
 *   <li>{@code execution.SqlExecutionCapability}（§6.20 EXECUTE）：后端无关只读执行，
 *       落实限行 / 超时 / 只读守卫，JdbcSqlExecutor 适配任意 javax.sql.DataSource；</li>
 *   <li>{@code chart.ChartGenerationCapability}（§6.20 CHART）：给定数据集与问题产出
 *       ECharts 风格可视化配置，缺模型时走启发式，失败时降级为表格。</li>
 * </ul>
 *
 * <p>设计约束：
 * <ul>
 *   <li>后端无关：只依赖本模块自定义的中立端口（LlmChatPort / SqlExecutorPort），
 *       不依赖任何具体 LLM 框架与驱动，由产品侧 adapter 落地；</li>
 *   <li>纯函数：给定相同输入结果可复现，生成不执行、执行不画图、画图不回答；</li>
 *   <li>显式输入：只接收上游注入的可见 schema / 约束 / 数据集，不在能力内再判权限；</li>
 *   <li>异常统一：业务异常继承 fw-plus-common 的 BizException，便于状态码与 i18n。</li>
 * </ul>
 */
package com.xiongx9527.fw.plus.ai.askdata;
