/**
 * fw-plus-ai-askdata — 数据问答（askdata）通用领域封装（DomainPack）。
 *
 * <p>参考 docs/ai-layer：把问数链路上与后端无关的“生成 / 执行 / 可视化”三段能力提成
 * 一等、可独立调用的能力，供内联问数 pipeline 与工具调用（MCP / function calling）共用。</p>
 *
 * <p>已封装能力：
 * <ul>
 *   <li>{@code capability.SqlGenerationCapability}（§6.38）：NL2SQL 纯函数 + 显式输入，
 *       本地优先校验 + LLM 语义校验 + 修复环；</li>
 *   <li>{@code execution.SqlExecutionCapability}（§6.20 EXECUTE）：后端无关只读执行，
 *       落实限行 / 超时 / 只读守卫，JdbcSqlExecutor 适配任意 javax.sql.DataSource；</li>
 *   <li>{@code chart.ChartGenerationCapability}（§6.20 CHART）：给定数据集与问题产出
 *       ECharts 风格可视化配置，缺模型时走启发式，失败时降级为表格。</li>
 * </ul>
 *
 * <p>对 docs/ai-layer 已提出问题（产品代码不动，仅在本 DomainPack 内防御性闭环）：
 * <ul>
 *   <li>§6.14 L1 护栏：本地规则（禁写 DML / 表名可见 / 括号平衡）先过，通过后再跑可选 LLM 语义校验；</li>
 *   <li>§6.36 质量基线“可执行”层：本地结构校验 + 真实 JDBC 执行能力（JdbcSqlExecutor）作为可执行性门禁；</li>
 *   <li>§6.37 全链路日志：三个能力只记元数据（行数 / 耗时 / 校验结论 / 置信度 / 图表类型），
 *       不记 SQL 原文、行数据或模型原文，避免泄露业务数据；</li>
 *   <li>风险 A.2.1（SSR 重新猜类型丢弃模型配置）：preferredType 优先级最高，仅在其缺失时回退 LLM / 启发式；</li>
 *   <li>风险 A.2.2（模型幻觉不存在的列名画空图）：新增字段存在性校验，引用列不在数据集中即降级为表格；</li>
 *   <li>风险 A.1.2（图表实际绘制未截断）：ChartDataset.MAX_CHART_ROWS 截断注入图表的数据集。</li>
 *   <li>§6.38.4 方言感知（DialectSqlChecker + LocalSqlValidationStrategy）：仅标记反引号误用、
 *       LIMIT/TOP 在方言不支持时的分页关键字错配，交由修复环按方言提示重出，而非静默改写不可信 SQL。</li>
 *   <li>§6.26 / D.4 LLM 韧性（ResilientLlmChatPort + SqlGenerationCapabilityBuilder.resilient）：
 *       同一模型内的指数退避重试（仅瞬时错误）+ 三态熔断（CLOSED/OPEN/HALF_OPEN），
 *       跨供应商故障转移仍属产品策略（§6.26.13），不由本装饰器承担。</li>
 *   <li>§6.22 输入归一（SqlGenInputNormalizer + SqlGenerationCapabilityBuilder.normalizeInput）：
 *       空白折叠 + 长度上限（fail-fast 不静默截断）+ 可注入的相对时间锚定前置改写，模块不内置语言相关日期解析。</li>
 *   <li>扩展 VERIFY（DefaultResultVerifier.primaryKeyColumn + ChartConfigValidator.isDatasetConsistent）：
 *       执行期按主键列名检测重复（列名来自 SchemaContext.primaryKeyColumn，§6.38.2），
 *       图表侧校验数据集矩形 / x 轴系列长度一致，非矩形即降级表格。</li>
 * </ul>
 *
 * <p>不在本模块范围（属产品侧接线 / 前端 / SSR，记录备查）：免登录图表图片与 TTL 跨租户、
 * title 未转义（v-html XSS）、SQL 外层 SELECT * 包裹丢失 ORDER BY、模型原文日志含业务数据等。</p>
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

