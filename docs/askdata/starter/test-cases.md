# AskData 测试用例

本用例集把现有 Core/Starter 的实现边界与业务接入验收边界分开。除标为“当前证据”的用例外，其余均是业务接入真实组件时必须执行的测试，不能作为外部系统已联通的证明。

## 执行约定

- 固定使用两个项目、两个操作人和至少一个只读测试数据源；全部断言请求、会话、记忆、推荐、日志和审计均带 `projectId + operatorId`。
- SQL 测试账户仅授予最小 `SELECT` 权限；测试数据不含真实密钥、完整 token、连接串或敏感结果。
- 集群、远程服务、DataEase、MCP、Dify、Coze、n8n、企业微信、钉钉、飞书使用隔离的桩服务或专用测试租户；没有授权时不得连接生产端点。
- 当前证据来自现有单元测试和本目录设计文档；未覆盖的接入项必须在对应模块创建 `*Test` 或 `*IT`，并把验证命令写入验证记录。

| ID | 类别 | 用例目标 | 前置 | 步骤 | 预期 | 关联 SQLBot/WrenAI 功能项 | 当前证据/后续补充 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UNIT-01 | 单元 | 验证项目与操作人上下文拒绝空白并规范化输入。 | 创建 `AskDataProjectContext`。 | 分别传 null、空白、带首尾空白的 projectId/operatorId。 | 空白抛稳定参数错误；合法值去除首尾空白。 | SQLBot 工作空间/项目、权限配置；WrenAI metadata constraints | 当前证据：Core 上下文单测；后续保留回归。 |
| UNIT-02 | 单元 | 验证能力注册表的 key、版本、优先级和集群兼容性。 | 构造多个 CapabilityProvider。 | 注册重复 key、越界版本、非集群能力；查询同类列表。 | 非法注册拒绝；列表按优先级降序再按 key 排序且不可修改。 | SQLBot 模型配置/插件能力；WrenAI validate、model substitute | 当前证据：`AskDataCapabilityRegistryTest`。 |
| UNIT-03 | 单元 | 验证多问题拆分、部分失败聚合和父检查点恢复。 | 提供成功/失败/超时子问题桩及合法父检查点。 | 提交显式列表和中英文分号复合问题；再以相同项目、操作人、授权版本、计划和路由快照恢复。 | 子问题独立终态并保留部分结果；只执行未完成节点，成功节点与已完成 SQL 不重放；身份、版本或快照漂移受控拒绝。 | SQLBot 多提问；WrenAI 后台任务、轮询结果 | 当前证据：`AskDataMultiQuestionFlowTest` 覆盖 DAG、部分失败、超时、取消、父检查点恢复和不重放。 |
| CONTRACT-01 | 契约 | 验证业务覆盖 Bean 不改变 Core 隔离、错误和审计契约。 | 提供自定义 AskDataService/模型 Provider。 | 以不同项目和操作人调用默认与覆盖实现。 | 覆盖实现仍接收并传递上下文；不泄露跨范围状态；错误为稳定 DTO。 | SQLBot 模型接入、项目权限；WrenAI 可嵌入 API | 后续补充：所有第三方 Provider 接入必须运行。 |
| CONTRACT-02 | 契约 | 验证 SQL 插件不得获得裸连接且插件输出必经最终校验。 | 注入记录型插件和恶意插件。 | 恶意插件将候选 SQL 改为写 SQL 或拼接参数。 | 插件只有受控命令；最终校验拒绝写入和拼接结果并写审计。 | SQLBot SQL 安全；WrenAI dry-plan、dry-run | 当前证据：SQL 安全文档要求；后续在 Core SQL 模块补契约测试。 |
| CONTRACT-03 | 契约 | 验证共享存储实现的项目/操作人隔离、租约、版本和幂等。 | JDBC/Redis/业务 Store 测试实现。 | 跨节点读写会话、检查点、Agent 状态；争抢同一租约。 | 跨范围读取为空/拒绝；版本冲突和租约失败受控；幂等键不重复执行。 | SQLBot 多轮上下文；WrenAI 后台任务、任务轮询 | 后续补充：持久化 SPI 接入前必测。 |
| AUTO-01 | 自动配置 | 验证默认 Starter 装配和 `@ConditionalOnMissingBean` 覆盖。 | Spring Boot 2.7.18 测试上下文。 | 无业务 Bean 启动；逐个注入模型、SQL、存储、AskDataService Bean 后启动。 | 默认 Bean 仅在缺失时装配；业务 Bean 生效；不传递 Java 17 专属依赖。 | SQLBot 安装部署、模型配置；WrenAI API 文档 | 当前证据：自动配置与默认服务相关单测；后续覆盖所有 SPI。 |
| AUTO-02 | 自动配置 | 验证配置默认值与非法集群内存组合。 | 两组 YAML。 | 默认启动；设置 `state.cluster-mode=true` 和 `state.store=memory` 启动。 | 默认采用单机内存；集群+memory 启动期失败并说明共享存储要求。 | SQLBot 安装部署；WrenAI 后台任务 | 当前证据：`configuration.md`；后续补配置绑定集成测试。 |
| SQL-01 | SQL 安全 | 验证单语句、只读、对象授权、限行与超时。 | 只读 JDBC 数据源、授权对象和可控慢查询。 | 提交 SELECT、分号多语句、INSERT/DDL、未授权表、无 LIMIT、慢查询。 | 只有授权单 SELECT 执行；强制最大 100 行或更严限制；其余拒绝并审计。 | SQLBot Text-to-SQL、数据源、SQL 安全；WrenAI dry-plan | 当前证据：`sql-safety.md` 与已有 SQL 校验边界；后续端到端 JDBC 测试。 |
| SQL-02 | SQL 安全 | 验证具名参数绑定、方言未知和审计脱敏。 | 含租户/操作人动态值的查询。 | 传入注入字符、未知方言和敏感参数。 | SQL 文本不拼接动态值；未知方言拒绝；审计不出现参数、token、连接串或原始模型响应。 | SQLBot 权限配置、Prompt；WrenAI validate、metadata constraints | 后续补安全回归与日志扫描。 |
| STORE-01 | 存储一致性 | 验证内存默认实现的容量、隔离和重启边界。 | 单机内存 Store。 | 超过会话检查点 100、监控日志 1000、路由快照 100 的容量；切换项目/人。 | 只在当前范围淘汰最早项；无跨范围混入；重启丢失符合文档。 | SQLBot 多轮上下文、长期记忆、操作日志；WrenAI 历史问题、遥测 | 当前证据：监控日志存储测试；后续补其余 Store。 |
| STORE-02 | 存储一致性 | 验证从内存迁移到共享存储不伪造可恢复状态。 | 演练环境、共享 Store。 | 排空请求、灰度写入、核验读取、模拟共享 Store 故障。 | 只迁移可验证状态；故障时拒绝状态一致性请求，不能回退某节点内存。 | SQLBot 安装部署/恢复；WrenAI 后台任务 | 后续补迁移演练 IT。 |
| MODEL-01 | 模型 fallback | 验证请求、项目、数据源、Agent、默认 key 的优先级。 | 注册各层不同模型 key。 | 分层设置 key 并发起请求。 | 命中最具体可用 key；路由审计记录实际层级，不泄露密钥。 | SQLBot AI 模型配置；WrenAI model substitute | 当前证据：模型路由设计；后续补完整参数化单测。 |
| MODEL-02 | 模型 fallback | 验证仅基础设施错误触发候选降级。 | 首个 Provider 分别返回超时、认证、预算和输入错误。 | 各执行一次问数。 | 超时/限流/网络可尝试后继且不重复同 key；认证/策略/输入/预算原样返回，不降级。 | SQLBot 模型接入；WrenAI Provider 模型、model substitute | 当前证据：`runtime-replacement.md`；后续补 Provider 契约测试。 |
| AGENT-01 | 多 Agent/recovery | 验证可重入节点恢复、租约和版本检查。 | 共享检查点、两节点执行器。 | 在无副作用节点中断后恢复；制造版本、权限、语义版本或租约冲突。 | 仅兼容且可重入节点恢复；冲突拒绝并写诊断。 | SQLBot 多提问/恢复；WrenAI 后台任务、轮询结果 | 当前证据：`flow-diagrams.md` 恢复约束；后续分布式 IT。 |
| AGENT-02 | 多 Agent/recovery | 验证 SQL/外部副作用不确定时绝不自动重放。 | 模拟执行中断及外部工具调用。 | 在 SQL 已发送或工具结果未知时请求恢复。 | 返回受控不确定/恢复失败，保留审计，等待业务授权。 | SQLBot SQL 安全；WrenAI dry-run、后台任务 | 当前证据：恢复和 Java 17 文档；后续故障注入测试。 |
| MEM-01 | memory/recommendation 权限 | 验证长期记忆不跨项目或跨操作人召回。 | 两项目两操作人写入相似记忆。 | 使用另一操作人或项目查询并尝试回退。 | 只返回同项目同操作人内容；撤销后不可读取。 | SQLBot 长期记忆；WrenAI 历史问题 | 当前证据：默认内存桥接与存储矩阵；后续持久化 SPI 契约。 |
| MEM-02 | memory/recommendation 权限 | 验证推荐画像、反馈与管理员查询权限。 | 写入不同人推荐和监控日志。 | 查询自己的推荐、其他人推荐和跨项目管理日志。 | 个人画像不复用；默认管理员仅可读授权项目；跨项目需宿主显式授权。 | SQLBot 推荐、操作日志；WrenAI 问题推荐、ask feedback | 当前证据：`index.md`、`storage-matrix.md`；后续权限 IT。 |
| AUD-01 | monitoring audit | 验证强制生命周期日志、脱敏和导出失败隔离。 | 记录型本地 Store 和失败的 Langfuse/LangSmith/OTel 导出器。 | 覆盖规划、会话、子问题、路由、Agent、SQL、恢复、取消、完成/失败事件及 Langfuse 容器关闭尾批。 | 每阶段有本地脱敏日志；外部导出失败不影响问数；敏感字段未落日志；关闭时未满批次被 flush。 | SQLBot 操作日志；WrenAI 遥测 | 当前证据：`AskDataObservabilityContractTest`、`VendorObservationExporterTest` 和 Flow 生命周期测试。 |
| J17-01 | Java 17 remote runtime | 验证协议 payload、认证、版本协商与错误映射。 | Java 8 主进程、Java 17 桩服务、mTLS/JWT/HMAC 之一。 | 调用 HTTP/SSE/MCP/A2A/gRPC 中已选协议，发送兼容/不兼容版本和各类错误。 | 仅脱敏 DTO 出站；版本不兼容拒绝；网络/超时可重试，认证/输入/协议错误不可重试。 | SQLBot 模型/MCP 接入；WrenAI 可嵌入 API、Provider 模型 | 当前证据：`java17-remote-runtime.md`；后续每个传输实现 IT。 |
| J17-02 | Java 17 remote runtime | 验证远程运行时不可扩权、不可改写路由、可灰度回退。 | 远程服务尝试更改项目、数据源或模型 key。 | 发送篡改响应、健康失败和熔断场景。 | 主进程拒绝越权字段；按项目/能力 key 灰度；回退至业务上一已验证的 LangChain4j 能力 key。 | SQLBot 模型路由/平台接入；WrenAI model substitute | 后续补安全与灰度集成测试。 |
| FE-01 | frontend integration | 验证控制台/嵌入式小助手只使用后端短期 token。 | Vue3 测试宿主页、token 服务桩。 | 以浮窗、全屏、区域嵌入加载；检查 localStorage、Cookie、Header。 | 不暴露 appSecret/宿主凭据；token 过期/越权被拒绝；项目/操作人上下文完整。 | SQLBot 小助手/嵌入式对接；WrenAI 可嵌入 API | 业务接入前端后补 Playwright 测试。 |
| FE-02 | frontend integration | 验证 DataEase、MCP、Dify、Coze、n8n 接入的权限与结果契约。 | 各调用方桩、DataEase `x-de-token` 与 AES-Key 测试密钥。 | 调用 datasource、问数、流式/轮询接口，模拟无权和密钥轮换。 | 只返回授权数据源和脱敏结果；DataEase 固定接口兼容；所有调用进入 SQL 校验和审计。 | SQLBot MCP、DataEase、Dify/Coze/n8n；WrenAI API/任务轮询 | 后续为 MCP/API/嵌入发布模块建立契约 IT。 |
| ENT-01 | frontend integration | 验证业务 IAM/JustAuth 完成企业微信、钉钉、飞书扫码/免登、组织同步后，将身份映射为 AskData 项目成员并完成配置审计。 | 测试租户或 JustAuth 回调桩。 | 由 JustAuth 执行登录、免登、同步、密钥轮换和成员撤销，再用已认证 `operatorId/projectId` 调用 AskData。 | AskData 不自研 OAuth/扫码协议；敏感配置只存引用；变更和失败写管理员审计。 | SQLBot 企业微信/钉钉/飞书/扫码/免登/组织同步；WrenAI 无直接产品项 | 业务 IAM/JustAuth 联调和 AskData 接口契约需要在测试租户运行。 |

## 最小验收顺序

1. 当前 Core/Starter 改动至少执行 `UNIT-*`、`CONTRACT-*`、`AUTO-*`、`SQL-*`、`STORE-*`、`MODEL-*`、`AGENT-*`、`MEM-*` 和 `AUD-*` 的已实现范围。
2. 接入任何 Java 17 运行时时，必须通过 `J17-*`，且不得以远程成功替代主进程 SQL 安全、项目权限或恢复授权验证。
3. 发布 MCP、DataEase、Dify、Coze、n8n 或企业微信、钉钉、飞书能力前，必须通过相应 `FE-*`/`ENT-*` 集成测试；缺少测试则矩阵状态维持“业务集成契约覆盖”。
