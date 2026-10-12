# nl2sql-admin 管理端插件 完整目标规格

> **文档角色**：本文档承接 brief「## 验收示例 → ### 管理端插件（nl2sql-admin）」小节与决策 D3/D13/D25/D33/D35/D37/D38/D39/D43，定义管理端 magic 插件的完整目标规格（Shape 产物，纯 markdown，非代码实现）。管理端配置写入共享配置存储，API 服务与管理端同进程或分应用部署均可；配置经缓存 + 变更刷新机制热生效。

管理端整体实现为一个 magic 插件（D3），实现参照 s3 插件（`fw-plus-magicapi-plugin-s3`）及其管理端模式（D13）。UI 采用 toolbars 页签模式注册全屏管理页，问数实例等资源配置沿用 datasources 资源表单模式。

## 1. 插件形态与模块结构

- 后端：`fw-plus-boot-autoconfigure` 下 `com.xiongx9527.fw.plus.autoconfigure.magicapi.plugin.nl2sql` 包体系（参照 magicapi plugin 既有结构），包含：
  - `Nl2sqlPluginConfiguration`（@Import 进 MagicAPIBaseAutoConfiguration）
  - `entity/` TableStructure、TerminologyItem、TrainingExample、PromptTemplate、PromptVersion、AuditLog、FeedbackItem、DatasourceSchemaExport 等实体（继承 MpBaseEntity）
  - `mapper/`、`service/` 对应服务：TableStructureService、VectorizeService、TerminologyService、DataTrainingService、PromptService、AuditService、FeedbackService、SchemaExportService、SchemaDriftService、CostBoardService
  - `web/` 管理端 Controller（前缀 `/magic/web/nl2sql/`，随 MagicController 鉴权）
  - `configstore/` 共享配置存储读写封装（D24/D25：配置缓存 + 变更刷新，同进程事件/分应用版本号轮询或 Redis 广播）
- 前端：新建 `fw-plus-magicapi-plugin-nl2sql` 插件模块，`src/console` 同一 iife.js 同时导出 datasources 与 toolbars；构建沿用 exec-maven-plugin + maven-resources-plugin 流程，产物为 iife.js 进 jar。
- SQL：根目录 `META-INF/sql/fw-plus-nl2sql.sql`，DDL 带 schema 版本号；分应用部署时版本不匹配明确报错。

## 2. 管理端范围（toolbars 页签模式 + datasources 资源表单）

### 2.1 toolbars 管理页

以 `{id, title, icon, order, component, adminOnly: true, allowClose, fullscreen}` 形态注册进 magic 控制台的全屏管理页，至少包含：

- `nl2sql-table-structure`：表结构管理
- `nl2sql-terminology`：术语库
- `nl2sql-data-training`：数据训练
- `nl2sql-prompt`：自定义提示词与版本化
- `nl2sql-datasource`：数据源 schema 管理与导入导出
- `nl2sql-feedback`：结果反馈待改进池
- `nl2sql-cost`：成本看板
- `nl2sql-audit`：操作审计查询
- `nl2sql-security-policy`：数据权限策略管理（策略 CRUD、规则绑定、启停；D13，详细行为由 nl2sql-security 的 spec 承接）

### 2.2 datasources 资源表单

- 问数实例资源沿用 magicapi datasources 资源表单模式：每个实例绑定数据源（业务库）、向量模型/向量库（embedding-model + embedding-store 资源 key，D10）、大语言模型（chat-model 资源 key）、记录存储数据源（D15）、chitchat-enabled 开关（D34）、确认/自动执行模式、行数上限、闲聊分支 RAG/MCP 配置（D34a）。
- 多实例（D8）：不同实例各自绑定配置，互不影响；配置变更触发实例动态重建（旧实例关闭，D27）。

## 3. 表结构登记与向量化

- 管理员登记表/列元数据：表名、列名、数据类型、业务注释（表注释、列注释）、是否启用。
- 登记内容写入共享配置存储的表结构表；触发「向量化」时将表名 + 注释 + 列信息拼装为嵌入文本，调用实例绑定的 EmbeddingModel 向量化，写入实例绑定的向量库，metadata 携带 datasource/table/columns 标识，供表匹配 RAG（D6 向量分数 + 关键词融合评分）召回。
- 重向量化：表结构或注释修改后需重新向量化；SchemaDriftService 检测到差异时提示管理员重向量化。
- 向量化状态（pending/vectorized/error）在管理页可见，失败可重试。

## 4. 术语库

- 管理员维护术语映射：业务术语 → 标准词/标准字段表达式（术语名、标准词、说明、启停）。
- 问数生成过程中：关键词提取后经术语库扩展为标准词，并注入提示词，确保 LLM 使用标准词生成 SQL。
- 支持模板导入导出（§9.2）。

## 5. 数据训练

- 管理员人工保存正确的问答-SQL 示例（问题、标准 SQL、关联数据源/实例、方言、启停）。
- 提问时按向量相似度召回相近示例注入 few-shot 提示词，提升生成命中率。
- 支持模板导入导出（§9.2）。

## 6. 自定义提示词与版本化

- 管理员可按实例（或全局）配置自定义提示词，生成时覆盖或追加默认提示词规则。
- 每次提示词或方言示例修改自动保存历史版本；管理页可查看历史版本内容与修改人，并支持回滚为历史版本（回滚即生成新版本）。

## 7. 管理端操作审计

- 管理端所有配置变更（表结构/术语/训练示例/提示词/数据权限策略/实例/数据源配置/加密开关等）记录审计日志：操作人、操作时间、对象类型与 id、变更前快照、变更后快照。
- 审计日志可按对象、操作人、时间段查询；与问数编排的 OpenTelemetry 调用轨迹互补（D32）。

## 8. 结果反馈闭环

- 用户端对问数结果点赞/点踩，反馈写入待改进池；点踩自动收集用户问题、生成的 SQL 与错误/不满意原因。
- 管理员在反馈管理页查看待改进池，可一键基于反馈补充术语映射或新增校准示例，形成运营闭环。

## 9. 结果导出补全（D43）

### 9.1 问数结果导出

- 对某条问数记录导出 Excel/CSV 文件下载（复用 EasyPoi）；可同时导出图表图片 PNG（服务端出图，D42）。

### 9.2 术语/训练示例模板导入导出

- 管理员导出术语库/训练示例为 Excel 模板文件；按模板回填后可导入；导入后按最新配置生效（经配置刷新机制热更新）。

### 9.3 数据源 schema 导入导出

- 导出某数据源的 schema 配置（已登记表结构与本地注释）为可迁移文件；在另一实例导入后复用表结构登记与注释。

## 10. schema 变更检测（D35）

- 支持定时与手动触发变更检测：对比业务库实际 schema（information_schema 查询）与已登记表结构。
- 差异类型：新表、新列、列类型变更；管理页提醒管理员补登记或重向量化，已登记但库中已删除的表/列同样提示。

## 11. 成本看板（D37）

- 记录库 token 用量与问数次数按用户/实例/时间段聚合展示（表格 + 简单图表）。
- 闲聊分支命中标记 chitchat 类型，不污染问数质量统计（D34）。

## 12. 共享配置存储写入（D24/D25）

- 管理端把配置（表结构/术语/训练示例/提示词/权限策略/问数实例/数据源与模型配置）写入共享配置存储数据库表；API 服务从同一存储读取。
- 敏感字段（数据源密码、LLM api-key、回调密钥）以加密形态写入，API 服务读取时解密。
- 配置缓存 + 变更刷新：同进程事件通知 / 分应用版本号轮询或 Redis 广播；配置即热生效（实例多实例动态重建语义）。
- 共享配置存储 DDL 由框架统一提供并带 schema 版本；分应用部署时版本不匹配明确报错。

## 13. 约束与非目标

- Java 1.8 语法（禁 Java 9+ API），包命名 `com.xiongx9527.fw.plus.*`，源文件 UTF-8。
- 管理端 capability 只覆盖管理侧配置与运营；数据权限策略的执行语义、用户端 UI、图表渲染、MCP 对外能力分别由 nl2sql-security / nl2sql-user-ui / nl2sql-chart-dashboard / nl2sql-integrations 承接。
- 不做用户管理与角色体系（沿用既有非目标）；认证授权对接框架 Sa-Token。
- 不复制 SQLBot 代码（类 GPLv3 许可约束，仅对齐功能设计）。

## 验收场景

- 表结构管理：WHEN 登记表/列及业务注释并触发向量化 THEN 表结构进入向量库可供表匹配（§3）
- 术语库：WHEN 配置术语映射后问数 THEN 生成过程使用标准词（§4）
- 数据训练：WHEN 人工保存正确 SQL 示例后提相似问题 THEN 生成命中该示例（§5）
- 自定义提示词：WHEN 配置自定义提示词 THEN 生成时覆盖/追加默认规则（§6）
- 提示词版本化：WHEN 提示词/方言示例修改后 THEN 可查看历史版本并回滚（§6）
- 管理端操作审计：WHEN 管理端变更配置（表结构/策略/提示词等） THEN 记录操作人与变更前后快照，可查询（§7）
- 结果反馈：WHEN 用户对问数结果点赞/点踩 THEN 反馈入待改进池（点踩自动收集问题与生成的 SQL），管理员可据此补术语/校准示例（§8）
- 结果导出：WHEN 用户对问数结果导出 THEN 生成 Excel/CSV 文件下载，并可同时导出图表图片（PNG）（§9.1）
- 术语/示例模板导入导出：WHEN 管理员导出术语库/训练示例 THEN 得到可按模板回填再导入的 Excel 文件；导入后按最新配置生效（§9.2）
- 数据源 schema 导入导出：WHEN 管理员导出某数据源的 schema 配置 THEN 可在另一实例导入，复用表结构登记与本地注释（§9.3）
- schema 变更检测：WHEN 触发变更检测（定时/手动） THEN 对比业务库实际 schema 与已登记表结构，差异（新表/新列/类型变更）提醒管理员补登记/重向量化（§10）
- 成本看板：WHEN 管理员查看成本看板 THEN 按用户/实例/时间段展示 token 消耗与问数次数统计（§11）
