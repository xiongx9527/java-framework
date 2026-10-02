# 目标

交付"easy-rag 知识库"的**设计文档定稿**（参照 Dify 知识库全功能），作为后续实现 change 的输入契约。本 change 不写实现代码：

- `specs/ai-knowledge-rag/spec.md`：完整目标规格（模块结构、数据模型、索引管道、管理端接口、对外 API 与认证授权、Dify 对接、knowledge 检索器、前端 UI、配置项）
- 三张表的完整 DDL 设计稿（实现 change 进入 Build 的第一批契约）
- 对外 API 与管理端同属 content-retriever 插件同一服务，API 实现 Sa-Token 认证授权，兼容 Dify External Knowledge API

功能实现（Java/前端代码）由后续**一个**实现 change 承接（worktree 隔离），以本 change 定稿的设计文档为输入。

# 范围

## Source coverage

需求来源：S-A = 用户对话多轮指示（2026-10-02）；S-B = 已批准实施计划 `/Users/xiongxiao/.codebuddy/plans/toasty-aurora-darwin-Pp3c1q2g.md`（complete，已逐节读取）；S-C = Dify External Knowledge API 官方规范（docs.dify.ai，complete，已读取——本 spec §3.3 的协议设计输入）。

| 来源条目与位置 | 读取状态 | 需要保留的内容 | Spec 位置 | 验收 ID | 覆盖状态 | 理由或替代关系 |
| --- | --- | --- | --- | --- | --- | --- |
| S1：S-A"magic-api的插件；管理端使用 magic（toolbars 模式）" | complete | 管理端为 magic 控制台 toolbars 页面，全功能 UI 管理 | spec §4.1 | A1 | covered | 设计需覆盖 |
| S2：S-A"langchain4j-easy-rag 依赖吧" | complete | 解析/切分底座用 langchain4j-easy-rag 0.35.0 | spec §2.3 | A1 | covered | 设计需覆盖 |
| S3：S-A"ContentRetriever 要多实例，注入 MagicDynamicContentRetriever" | complete | knowledge 检索器懒解析多实例，同 MagicDynamicContentRetriever 模式 | spec §3.2 | A1 | covered | 设计需覆盖 |
| S4：S-A"fw-plus-ai-magicapi-plugin-langchain4j-content-retriever 上面扩展；content-retriever 的一个类型" | complete | knowledge 是 content-retriever 的 provider 短名；前端表单扩展到现有 content-retriever 插件 | spec §3.2、§4.2 | A1 | covered | 设计需覆盖 |
| S5：S-A"不加 fw-plus-ai-magicapi-plugin-knowledge；只扩展 content-retriever" | complete | 不新增任何 Maven 模块；前端全部在 content-retriever 插件 console | spec §1、§4 | A1 | covered | 设计需覆盖 |
| S6：S-A"提供一个UI管理" | complete | 控制台 toolbars 管理页面：数据集/文档/分段/召回测试 | spec §4.1 | A1 | covered | 设计需覆盖 |
| S7：S-A"还有一个api 服务，业务方可对接" | complete | 对外 REST API 设计（入库/检索/状态查询），由本插件同一服务提供 | spec §3.3 | A3 | covered | 当前有效需求 |
| S8：S-A"这个服务可以给 dify 提供rag 来源" + "这个只是api 能力之一" | complete | Dify External Knowledge API 对接设计：POST /retrieval、records 响应、错误码，API 服务的能力之一 | spec §3.3 | A3 | covered | 协议按 S-C 官方规范 |
| S9：S-A"原始数据要保存吧" | complete | 原始文档持久化设计（KbOriginalFileStore） | spec §2.4 | A1 | covered | 设计需覆盖 |
| S10：S-A"需要配置jdbc数据源吧" | complete | 三张元数据表落应用主数据源 + MyBatis-Plus；无数据源不装配 | spec §2.1 | A1、A2 | covered | 设计需覆盖 |
| S11：S-A"ContentRetriever 要区分类型，其它类型是无法管理rag的" | complete | 知识库管理端只管理 knowledge 类型 | spec §3.2、§4.1 | A1 | covered | 设计需覆盖 |
| S12：S-B 计划全文（模块结构/数据表/索引管道/接口清单/分期/风险） | complete | 数据模型、索引状态机、接口清单、风险约束 | spec 全文 | A1、A2 | covered | 计划已批准，设计需覆盖 |
| S13：S-A"第一期功能范围：dify 的全功能" | complete | 功能全集含父子分段、QA 模式、rerank | spec §2.3 | A1 | covered | 设计需覆盖 |
| S14：S-A"api 认证授权使用 sa-token；业务方怎么对接不管" | complete | 对外 API 的 Sa-Token 认证授权设计；凭证/客户端管理复用现有 magicapi satoken 插件，业务方如何获取凭证不在范围 | spec §3.3 | A3 | covered | 当前有效需求 |
| S15：S-A"注意 Tokenizer，高版本要替换为 TokenCountEstimator" | complete | token 计数隔离封装设计（KbTokenCounter） | spec §2.3 | A1 | covered | 设计需覆盖 |
| S16：S-A"分2个change吧，管理端 + API 服务；2个是独立的，只是数据库共享" | complete | （历史决定）曾拆分 API 为独立服务/change | — | — | superseded | 用户 S20/S21 修订：API 并入同一服务，由 S20/S21 替代 |
| S17：S-A"按你的建议吧"（采纳全部架构建议） | complete | 索引入库与执行解耦（waiting + 即时触发 + 轮询兜底）、CAS 认领、embedding_model_active、字符切分+tokens 近似、DDL 先行 | spec §2.2、§2.3 | A1、A2 | covered | 架构建议采纳（2026-10-02） |
| S18：S-A"管理端要有 dify 的所有功能" + 盘点决定 | complete | 文档 metadata 纳入（含检索过滤）；权限不做；归档/批量/下载/分隔符/关键词纳入 | spec §2.2、§3.1、§3.2、§4.1 | A1、A2 | covered | Dify 全功能对齐补充（2026-10-02） |
| S19：S-A 方案 A：本 change 范围重定义为纯设计文档交付 | complete | 本 change 只交付设计文档定稿；功能实现由后续 change 承接 | brief 全文 | A1–A4 | covered | 范围重定义（2026-10-02） |
| S20：S-A"api不要单独的服务" | complete | 取消 easy-rag-api 独立服务：对外 API 并入本插件同一服务 | spec §1、§3.3 | A1、A3 | covered | 架构修订（2026-10-02），替代 S16 拆分 |
| S21：S-A"理论上是直接使用 fw-plus-ai-magicapi-plugin-langchain4j-content-retriever 对我提供接口，只是 api 需要实现认证授权" | complete | 对外接口由 content-retriever 插件直接提供（同一插件内 KbOpenApiController）；API 必须实现认证授权（Sa-Token） | spec §3.3 | A3 | covered | 架构修订（2026-10-02） |

## 交付物

- `specs/ai-knowledge-rag/spec.md`：完整目标规格（本 change 主体设计文档，含对外 API 与认证授权设计）
- 完整 DDL 设计稿（内嵌 spec §2.2，实现 change 进入 Build 时原样落地为 `META-INF/sql/fw-plus-ai-knowledge.sql`）

# 非目标

- 不写任何实现代码（Java/前端/console）
- 不执行建表、不触碰运行环境
- 不在本 change 内创建实现 change（后续按本文档另起一个实现 change）
- ES 混合检索、economy 经济模式、多租户/数据权限、知识库权限、Dify 写回、业务方凭证发放流程（认证凭证由现有 satoken 插件管理）

# 验收示例

- A1 设计文档完整：specs/ai-knowledge-rag/spec.md 覆盖全部已确认设计——模块与包结构、三表数据模型（含 archived/metadata/keywords/embedding_model_active/segment 切分参数等全部字段）、索引管道（入库与执行解耦/CAS 认领/父子分段/QA 模式/嵌入一致性）、管理端接口清单（数据集/文档/归档/批量/下载/metadata/分段/召回测试）、knowledge 检索器（多实例/类型边界/过滤链）、对外 API 与认证授权（§3.3）、前端 UI（toolbars 页面与 knowledge 类型表单）、配置项与约束；用户审查确认通过
- A2 DDL 设计稿定稿：三表完整 CREATE TABLE 语句（字段/类型/索引/唯一约束）内嵌 spec §2.2，可直接作为实现 change 的 `META-INF/sql/fw-plus-ai-knowledge.sql`，字段语义说明完备
- A3 对外 API 设计完整：§3.3 成文——对外 REST 接口（upload-by-file/create-by-text/状态查询/retrieve，由 content-retriever 插件同一服务提供）、Sa-Token 认证授权（校验点、未认证拒绝、复用 satoken 插件凭证管理）、Dify External Knowledge API 兼容（POST /retrieval、knowledge_id 路由、records 响应格式、错误码 1002/2001）
- A4 需求共识完备：brief 无 [blocking]，S1–S21 全部来源条目 covered（superseded 条目有替代说明），全部关键决策（D1–D17）有记录

# 约束与不变量

- 设计约束传递给实现 change：JDK 1.8（禁 Java 9+ API）；Spring Boot 2.7.18；不新增 Maven 模块；前端只改 content-retriever 插件 console；easy-rag/tika 依赖 optional；8 家向量库能力差异降级；显式 JDK8 线程池；langchain4j 0.35.0 版本不修改
- 本 change 产物仅为 Markdown 设计文档，随 git 提交于 change 分支

# 决策

- D1 形态：magic-api 生态扩展，不新增 Maven 模块（用户 S5）
- D2 管理端：控制台 toolbars 模式（s3-browser 同款结构，adminOnly）（用户 S1/S6）
- D3 底座：langchain4j-easy-rag 0.35.0（用户 S2）
- D4 检索器：ContentRetrieverProvider 短名 `knowledge`，懒解析多实例（用户 S3/S4）
- D5 类型边界：知识库管理端只管辖 knowledge 类型 retriever（用户 S11）
- D6 数据源：元数据表落应用主数据源 + MP；无数据源不装配（用户 S10）
- D7 原始文件：本地目录存储（fw.plus.ai.knowledge.storage-path，默认 ${user.home}/.fw-plus/kb），接口预留对象存储扩展（用户 S9）
- D8 交付形态（修订）：不拆分独立 API 服务——对外 API 由 content-retriever 插件同一服务直接提供（KbOpenApiController 与管理端同在插件内），实现时管理端 + API 一个实现 change 交付（用户 S20/S21）
- D9 Token 计数兼容：KbTokenCounter 单一封装点，M1 字符近似，升级换 TokenCountEstimator（用户 S15/S17）
- D10 入库与索引执行解耦：管理端与对外 API 入库均只置 waiting + 存原始文件，索引执行方扫描处理（入库即时触发 + 定时轮询兜底）（用户 S17/S20）
- D11 并发认领：状态流转 CAS，多实例不重复索引（用户 S17）
- D12 嵌入模型一致性：embedding_model_active 记录生效模型，检索与向量一致，切换提示重建（用户 S17）
- D13 DDL 先行：实现 change Build 第一批落地 DDL，字段语义冻结（用户 S17）
- D14 文档 metadata 完整纳入：键值对 + 检索过滤（向量库 filter 优先/内存兜底，格式对齐 Dify metadata_condition）（用户 S18）
- D15 权限：知识库权限不做（用户 S18）
- D16 低成本功能全部纳入：归档/批量操作/下载/自定义分隔符/分段关键词（用户 S18）
- D17 范围重定义（方案 A）：本 change 只交付设计文档定稿；功能实现由后续一个实现 change 承接，实现以本 change 定稿文档为输入（用户 S19）

# 待解决问题

（无）

# 验证预期

- 文档审查：用户逐节审查 spec.md 与 DDL 设计稿，确认后进入归档
- 一致性检查：spec 章节引用、验收编号、来源覆盖表三者对应无遗漏
- 归档后设计文档随 git 进入 dev 分支，作为后续实现 change 的输入
