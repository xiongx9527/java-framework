# 目标

按已定稿的设计文档（easy-rag-knowledge change 交付，dev 分支 `docs/comet/archive/2026-10-02-easy-rag-knowledge/`）**完整实现 easy-rag 知识库功能**：管理端（magic 控制台 toolbars UI + 管理接口）、索引管道、knowledge 类型检索器、对外 REST API（含 Sa-Token 认证授权与 Dify External Knowledge API 兼容）。管理端与对外 API 同属 content-retriever 插件同一服务，不新增 Maven 模块。

# 范围

## Source coverage

需求来源：S-DESIGN = 归档设计文档 `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/docs/comet/archive/2026-10-02-easy-rag-knowledge/specs/ai-knowledge-rag/spec.md`（complete，已逐节读取）及其 brief（complete，S1–S21 需求共识、D1–D17 决策）；S-DBRIEF = `docs/comet/archive/2026-10-02-easy-rag-knowledge/brief.md`。

| 来源条目与位置 | 读取状态 | 需要实现的内容 | Spec 位置 | 验收 ID | 覆盖状态 | 理由或替代关系 |
| --- | --- | --- | --- | --- | --- | --- |
| S-D1：S-DESIGN §1 模块与包结构 | complete | knowledge 包全量类清单 + 前端扩展点 + SQL 文件位置 | spec §1 | A1 | covered | 实现蓝图 |
| S-D2：S-DESIGN §2.1 装配与数据源 | complete | 主数据源 + MP、@ConditionalOnBean(SqlSessionFactory)、表存在性校验、fw-plus-mp-base 依赖 | spec §2.1 | A2 | covered | 实现蓝图 |
| S-D3：S-DESIGN §2.2 三表 DDL | complete | META-INF/sql/fw-plus-ai-knowledge.sql 原样落地（Build 第一批）+ 实体/Mapper | spec §2.2 | A1、A2 | covered | 实现蓝图 |
| S-D4：S-DESIGN §2.3 解析切分嵌入管道 | complete | Tika 解析、KbTokenCounter 字符近似、入库与执行解耦（waiting+即时触发+轮询）、CAS 认领、DocumentSplitters.recursive、ParentChildSplitter、QaExtractor、embedding_model_active 一致性、状态机、重建索引、KbIndexingExecutor | spec §2.3 | A3–A6、A11–A13 | covered | 实现蓝图 |
| S-D5：S-DESIGN §2.4 原始文件存储 | complete | KbOriginalFileStore + LocalKbOriginalFileStore | spec §2.4 | A7 | covered | 实现蓝图 |
| S-D6：S-DESIGN §3.1 管理端接口 | complete | KbConsoleController 全部接口与行为（含归档/批量/下载/metadata/retrievers 引用） | spec §3.1 | A5、A8、A9、A10 | covered | 实现蓝图 |
| S-D7：S-DESIGN §3.2 knowledge 检索器 | complete | KnowledgeContentRetrieverProvider（短名 knowledge）+ 懒解析多实例 + 过滤链（enabled/archived/metadata/score）+ ContentRetrieverProviderRegistry 注册 | spec §3.2 | A8 | covered | 实现蓝图 |
| S-D8：S-DESIGN §3.3 对外 API | complete | KbOpenApiController 四接口 + KbSaTokenAuthChecker（Sa-Token，复用 satoken 插件凭证管理） | spec §3.3 | A14、A15 | covered | 实现蓝图 |
| S-D9：S-DESIGN §3.4 Dify 兼容 | complete | POST /retrieval、knowledge_id 路由、records 响应（metadata 恒为对象）、错误码 1002/2001 | spec §3.4 | A16 | covered | 实现蓝图 |
| S-D10：S-DESIGN §4 前端 | complete | toolbars 知识库管理页（四视图）+ knowledge 类型表单 + i18n，同一 iife.js | spec §4 | A12、A13 | covered | 实现蓝图 |
| S-D11：S-DESIGN §5 配置项 | complete | fw.plus.ai.knowledge.enabled / storage-path | spec §5 | A2 | covered | 实现蓝图 |
| S-D12：S-DESIGN §6 非功能约束 | complete | JDK8、optional 依赖、8 家 store 降级、不自动建表 | spec §6 | A17 | covered | 实现蓝图 |
| S-D13：S-DBRIEF 设计决策 D1–D17 | complete | 全部设计决策在本实现中遵循（不重新讨论） | spec 全文 | A1–A17 | covered | 已定稿约束 |

## 实现范围

- 后端：fw-plus-ai-boot-autoconfigure 的 content-retriever 后端包 langchain4j/retriever 下新增 knowledge 子包（entity/mapper/service/pipeline/store/retriever/web/auth）+ KnowledgePluginConfiguration 装配 + pom 依赖（fw-plus-mp-base、mybatis-plus 均 optional）
- SQL：根目录 META-INF/sql/fw-plus-ai-knowledge.sql
- 前端：fw-plus-ai-magicapi-plugin-langchain4j-content-retriever 的 src/console 扩展（toolbars 页面 + knowledge 类型表单 + i18n）
- magic-script：spring bean 方式 `import knowledge` 调 `search(datasetKey, query)`
- 单元/集成测试：管道（InMemory store）、CAS 认领、metadata 过滤、Dify 协议响应格式

# 非目标

- 不修改 magic-editor 主控台源码
- 不实现 ES 混合检索、economy 经济模式、知识库权限/多租户、Dify 写回
- 不实现业务方凭证发放流程（复用现有 satoken 插件）
- 不自动建表
- langchain4j 版本升级（维持 0.35.0；KbTokenCounter 已为 TokenCountEstimator 预留封装点）

# 验收示例

- A1 模块结构落地：knowledge 包按 §1 清单建成，`META-INF/sql/fw-plus-ai-knowledge.sql` 与设计 DDL 一致（Build 第一批）
- A2 装配与配置：无 SqlSessionFactory 时知识库不装配且其余 langchain4j 功能不受影响；两个配置项生效；实体/Mapper 继承 MpBaseEntity/MpBaseMapper
- A3 文档索引管道：上传后异步 完成 waiting→parsing→splitting→indexing→completed，向量写入指定 store（InMemory 集成测试验证）；CAS 认领保证并发不重复索引（测试验证）
- A4 索引失败与重建：解析/嵌入失败置 error 且 error_msg 可查；reindex 删旧分段与向量后重新入队成功
- A5 分段管理：列表/编辑（自动重嵌入）/启禁/删除生效；禁用分段不被检索返回
- A6 召回测试：hitTest 返回分段与 score，hit_count 累加
- A7 原始文档保存：上传文件落盘且路径写回 file_path；重建索引回读原文；删除文档同步删文件
- A8 knowledge 检索器：provider=knowledge 按 key 构建多实例；过滤 enabled/archived、score 阈值、metadata 条件生效（InMemory 测试验证）
- A9 管理端扩展接口：归档/取消归档、批量操作、下载、metadata 设置均可用且行为符合 §3.1
- A10 分段关键词与自定义分隔符：keywords 可编辑；segment_delimiter 切分生效
- A11 父子分段：父段落表不嵌入，子段嵌入带 parent_id，检索命中子段返回父段内容
- A12 控制台 UI：toolbars 知识库管理页（数据集/文档/分段/召回测试四视图）构建成功并注册（adminOnly）
- A13 knowledge 类型表单：content-retriever 表单 provider 选项含 knowledge 及专属 options；i18n 中英文齐备
- A14 对外 REST API：四接口可用（upload-by-file/create-by-text/状态查询/retrieve），入库走 waiting + 异步索引
- A15 Sa-Token 认证授权：KbSaTokenAuthChecker 校验请求；未认证请求被拒绝（统一拦截响应）
- A16 Dify 协议兼容：/retrieval 请求响应格式逐字段符合 §3.4；错误码 1002/2001；metadata 恒为对象
- A17 构建与约束：fw-plus-ai 层 mvnw compile 通过（JDK8 语法）；前端 npm build 产出 iife.js；无 Java 9+ API；easy-rag/tika optional

# 约束与不变量

- JDK 1.8 语法（禁 Java 9+ API/var/List.of）；Spring Boot 2.7.18；langchain4j 0.35.0 版本不修改
- 不新增 Maven 模块；前端只改 content-retriever 插件 console
- Tokenizer 调用收敛到 KbTokenCounter 单一封装点
- easy-rag/tika 依赖 optional=true；tika 版本由 BOM 管理
- 8 家向量库删除/metadata 能力差异按 store 分支降级
- 不自动建表；DDL 原样落地设计稿
- 遵循仓库编码风格：Tab 缩进、com.xiongx9527.fw.plus.* 包名、UTF-8

# 决策

- 全部沿用归档设计文档的 D1–D17 决策（见 S-DBRIEF），本 change 不重新讨论设计问题；实现细节偏离设计时，以设计文档为准，确需偏离先记录理由并同步回 spec
- 2026-10-02 用户决定：knowledge 的 Java 后端实现归属 content-retriever 插件的后端包，即 autoconfigure 的 `langchain4j/retriever/knowledge` 子包（与既有 ContentRetrieverProvider 实现同包体系），不新建顶级 `plugin/knowledge` 包，也不在 starter 模块目录放 Java 源码；业务（管理端/管道）、UI（console）、检索器后端在同一插件闭环

# 待解决问题

（无）

# 验证预期

- `fw-plus-ai` 层 `./fw-plus-ai/mvnw compile -DskipTests`（或根目录 mvnw -pl）通过
- content-retriever 插件 console npm build 成功产出 iife.js
- 管道/检索器/过滤/Dify 协议：JUnit 测试（InMemory embedding store + 近似嵌入模型）覆盖 A3/A4/A5/A8/A16
- A12/A13 前端以构建产物 + 代码审查验证；A14/A15 以代码审查 + 单测验证（无真实 Sa-Token 环境时以 mock/反射校验逻辑）
