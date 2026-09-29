---
change: langchain4j-magicapi-model-plugins
design-doc: docs/openspec/changes/langchain4j-magicapi-model-plugins/design.md
base-ref: 5240c9f5b2545adc20ee614735861074beb778e2
archived-with: 2026-09-29-langchain4j-magicapi-model-plugins
---

<!-- comet-task-authority: docs/openspec/changes/langchain4j-magicapi-model-plugins/tasks.md -->

# 实施计划：LangChain4j 模型资源 Magic-API 插件

任务完成状态以 `tasks.md` 为准，本文件说明实施方法与顺序，不维护第二套勾选框。

## 起点与既有成果

- 已提交（`2322ef973`、`5240c9f5b`）：`plugin/langchain4j/**` 下的 `model`（7 实体 + 基类 + `Langchain4jModelTypes`）、`resource`（抽象基类 + 7 storage）、`registry`（抽象基类 + 7 registry）、`factory`（SPI + 注册表 + 22 家工厂）、`support`（`Langchain4jModelHolder`、`Langchain4jOptionsReader`）、`web`（Controller、启动重试）、单类装配 `Langchain4jDatasourceConfiguration`。
- 未提交：`modules/langchain4j/`（`Langchain4jModule` 261 行旧版、`Langchain4jConfiguration`、`tool/**` 10 个类与 5 个测试）及 `MagicAPIBaseAutoConfiguration` 的 `@Import`。
- 本 change 相对既有实现的结构性变更：共享 holder → 七个 `MagicDynamic*DataSource`；单类装配 → 父装配 + 七个子 Configuration；`Langchain4jModule` 只读化；补齐前端子工程与全部测试。

## 通用约束

- Java 8：不得使用 `var` / `record` / `List.of()` / `Map.copyOf()` 等；注释与异常信息用简体中文。
- core 层 `fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j-*` 零修改（含修饰符）。
- 对外命名一律含 `langchain4j`，不得残留 `uid`。
- 构建/测试在 `fw-plus-ai` 层执行：单测 `./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=<TestClass> -nsu`；本地仓库缺依赖先 `./mvnw install -DskipTests -nsu`。根目录可用 `./mvnw` 全量构建。
- TDD：每个实现任务先给出因待实现行为而失败的测试命令与实际失败输出（RED），再实施并给出通过输出（GREEN）。行为保持型重构先补特征测试。
- 提交粒度：每任务一次提交，message 用 `feat(ai):` / `refactor(ai):` / `test(ai):` 前缀。
- 审查重点（跨任务固化）：options 键名错误必须在构建期抛出含完整路径的中文异常；同 key 重复保存时旧实例必须已被驱逐；`enabled=false` 只清理；启动顺序不利时 store 能自动恢复；关闭异常不阻断其它注销；未知 provider 在校验阶段被拒并列出可用短名。

## 1. 结构对齐：运行时持有者与装配

<!-- comet-task-ref:7c931e83-14a9-44a0-96d4-0b71a17409eb -->
### 1.1 七个 `MagicDynamic<Type>DataSource`

- 范围：在 `plugin/langchain4j/model/` 新增七个持有者（`MagicDynamicLangchain4jChatModelDataSource` 等），`implements DisposableBean`，按 `key` 提供 put/get/evict/keys/destroy；仅依赖本类型实例类型。
- 依赖：无（起点任务）。
- 约束：与 `MagicDynamicKafkaDataSource` 同构；驱逐即关闭，关闭异常仅记日志并继续；不跨类型共享状态。
- 验收：新增持有点测试（驱逐即关闭、关闭异常不阻断、`destroy()` 清空全部），PR 后测试通过；同时七个 `*MagicDynamicRegistry` 改注入本类型持有者（见 1.2 的清理）。

<!-- comet-task-ref:bb60100e-53ab-46ac-8c00-8bd6426f84b1 -->
### 1.2 移除共享 holder

- 范围：删除 `support/Langchain4jModelHolder.java`，把七个 registry 与 `Langchain4jDatasourceConfiguration` 的引用改为对应类型的持有者。
- 依赖：1.1。
- 约束：一次提交内完成替换，避免中间态出现两套实例来源；不保留兼容壳。
- 验收：`grep -r "Langchain4jModelHolder" fw-plus-ai` 无结果；`./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure compile -nsu` 通过。

<!-- comet-task-ref:9711f454-4605-4a31-a66d-0c354cd91916 -->
### 1.3 装配拆为父装配 + 七个顶层子 Configuration

- 范围：`Langchain4jDatasourceConfiguration` 保留 `plugin()`、`controllerRegister()`、工厂注册表等共享 bean，`@Import` 引入七个按类型的顶层子配置类（如 `Langchain4jChatModelDatasourceConfiguration`），每个子配置只声明本类型的持有者、storage、registry。
- 依赖：1.1、1.2。
- 约束：`@ConditionalOnMissingBean` 语义保留；单类型装配异常不得阻止其它类型；父类不直接声明类型专属 bean。
- 验收：新增装配测试断言七类持有者/storage/registry bean 齐备，并通过单类型故障隔离用例；测试通过。

<!-- comet-task-ref:e63751b7-027a-4532-a41d-2b63c11cff8b -->
### 1.4 目录分层核对

- 范围：确认 datasource 插件在 `plugin/langchain4j/**`、脚本模块与工具桥在 `modules/langchain4j/**`，修正与 design.md D7 不一致的落点。
- 依赖：1.3。
- 约束：移动只调整包与文件位置，不改变行为；更新受影响的 import 与测试包名。
- 验收：目录清单与 D7 一致且模块编译通过。

## 2. 脚本模块只读化（BREAKING）

<!-- comet-task-ref:0fa0f29a-16ed-479d-9843-5839d349d565 -->
### 2.1 `Langchain4jModule` 只读取用

- 范围：`@MagicModule("langchain4j")`；删除全部 `build*` 与内部实例 map；七个 `get*` 委托对应类型持有者；未配置时抛中文异常并列出该类型已配置 `key`。
- 依赖：1.1、1.2。
- 约束：不提供脚本侧构建入口；异常信息含类型与 `key`；`chat(...)` / `Assistant` 行为不变。
- 验收：新增模块测试覆盖委托、未配置异常（含类型与 `key`、列出可用 `key`），测试通过。

<!-- comet-task-ref:4f517478-07a8-4956-98c3-d647a85fb675 -->
### 2.2 驱逐与关闭语义

- 范围：`remove(type, key)` 改为持有者驱逐（不删除已保存配置）；`destroy()` 清空全部持有者并关闭可关闭实例。
- 依赖：2.1。
- 约束：驱逐后重新加载同一数据源应可再次取用。
- 验收：模块测试中「驱逐后可重新加载」「关闭时清空并关闭」用例通过。

## 3. provider 工厂与依赖

<!-- comet-task-ref:9d6b483c-fe1c-458b-8521-ad725e654262 -->
### 3.1 BOM 与 optional 依赖

- 范围：AI 层 BOM 统一管理 langchain4j 供应商版本；`fw-plus-ai-boot-autoconfigure` 以 `optional` 引入模型、分词器、向量库 jar（含内置向量模型）。
- 依赖：无（可与 1.x 并行）。
- 约束：个别社区版构件若以更高 JDK 编译，改用与 0.35.0 同代的可用实现，不提升 Java 基线；core 层不新增/删除依赖。
- 验收：`./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-dependencies install -DskipTests -nsu` 与 autoconfigure 编译通过。

<!-- comet-task-ref:ef795f81-e186-42b7-bcab-e3d84189025d -->
### 3.2 短名表由能力探测生成

- 范围：存储校验用短名集合改为从已注册工厂探测，与 `Langchain4jModelFactoryRegistry` 同源。
- 依赖：3.1。
- 约束：不得维护第二份短名常量表。
- 验收：新增工厂注册表测试覆盖「每个短名都有工厂」「工厂类可加载」「表外短名被拒」「工厂方法集合与资源类型表一致」，测试通过。

<!-- comet-task-ref:733eaa78-07a3-4c71-b272-1ee357739f43 -->
### 3.3 代表工厂单测

- 范围：为每个资源类型的构建方法补代表单测（builder 链参数透传、默认值语义、读取器缺参报错）。
- 依赖：3.2。
- 约束：每个资源类型至少一个代表工厂；断言到 builder 参数而非仅非空。
- 验收：OpenAI 与通义千问工厂测试通过。

<!-- comet-task-ref:d3edaff6-4f2e-4411-846c-5b34edd6de4e -->
### 3.4 选项读取器完整路径报错

- 范围：`Langchain4jOptionsReader` 缺失必填项时的异常信息含 `langchain4j-chat-model[qwen-main].options.api-key` 形式路径；覆盖 Duration / List / 枚举 / 数值转换。
- 依赖：无。
- 约束：异常为中文且可定位到类型、key、选项路径。
- 验收：新增读取器测试断言路径格式与各类型转换，测试通过。

## 4. embedding-store 依赖与启动恢复

<!-- comet-task-ref:c0ff527c-c590-47f7-86bf-f8f9cfc0f1fc -->
### 4.1 `embeddingModelKey` 必填与依赖解析

- 范围：storage 校验 `embeddingModelKey` 必填；注册时从 embedding-model 持有者取实例，取不到则注册失败并提示需先配置并启用对应 embedding-model。
- 依赖：1.1、1.3。
- 约束：失败不产生孤立实例；不做级联删除。
- 验收：store 注册器测试覆盖缺失/未启用依赖的失败用例。

<!-- comet-task-ref:04164d3b-6afd-4331-ac53-6688534784bf -->
### 4.2 维度决策链

- 范围：`options.dimension` → 被引用模型维度 → 中文报错。
- 依赖：4.1。
- 约束：三级顺序不可调换；都不可得时报错而非静默取默认值。
- 验收：同一测试类覆盖三条分支。

<!-- comet-task-ref:cfcadf67-5b71-4009-ad99-14127edd096a -->
### 4.3 启动完成重试注册

- 范围：启动完成钩子对启动期失败的 embedding-store 统一重试一次注册。
- 依赖：4.1。
- 约束：已成功的资源在重试时保持可用且不产生重复实例；与既有 `Langchain4jStartupRegistrar` 合并实现，不新增并行机制。
- 验收：新增启动重试测试覆盖「store 先于 model 加载后自动恢复」与「已成功不重复」，测试通过。

## 5. 连接测试 Controller

<!-- comet-task-ref:0bb739d2-45e7-4b94-8353-4c16c14ae71d -->
### 5.1 `POST /langchain4j/{type}/datasource/test`

- 范围：七类各自最小探测；未保存配置可测且不落库；streaming 两类只做构建验证；embedding-store 探测结束后关闭临时实例；返回 `ok` 或含类型/`key`/选项路径的中文错误。
- 依赖：4.1、4.2。
- 约束：测试不改变已保存资源与已注册实例；协议对齐既有 datasource test 的 `ok` 语义。
- 验收：新增 Controller 测试覆盖七类、错误、未保存、关闭分支。

## 6. 前端 console 子工程

<!-- comet-task-ref:0a8cecea-c192-47c0-b704-b20936305de1 -->
### 6.1 子工程与构建链路

- 范围：新建 `fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j`（pom 仿 MCP 子工程），`exec-maven-plugin` 执行 npm 构建，`maven-resources-plugin` 复制产物到 `target/classes/magic-editor/plugins/`。
- 依赖：无。
- 约束：构建失败不静默；产物名固定 `magic-langchain4j.1.0.0.iife.js`。
- 验收：`./mvnw -pl <子工程> package -nsu` 后产物文件存在且非空。

<!-- comet-task-ref:141e9aec-cead-4f4a-a489-e9806fe6648f -->
### 6.2 console 源码

- 范围：注册七类 datasource 的 `index`、七个 Vue 组件、`(资源类型 × provider)` 字段元数据表、连接测试 service、中英文 i18n；`embedding-store` 组件含 `embeddingModelKey` 选择项。
- 依赖：6.1、5.1（接口契约）。
- 约束：元数据表覆盖七类且每个已声明短名都有字段定义；字段键名 kebab-case。
- 验收：元数据表与七类覆盖清单核对通过，前端构建产物包含七类组件。

<!-- comet-task-ref:459d3315-bd41-44ad-bb45-2a9dd4c799bf -->
### 6.3 前端产物缺失降级

- 范围：前端产物不在约定路径时，只按基础插件标识注册且不抛异常。
- 依赖：6.1。
- 约束：降级只影响前端资源，服务端装配照常。
- 验收：装配测试中断言资源缺失路径不抛异常。

## 7. 工具桥（api / function → LangChain4j ToolProvider）

<!-- comet-task-ref:b8e1540b-aa96-459f-a67f-cfeb09ae486f -->
### 7.1 工具快照注册表

- 范围：两类资源独立启停；排除分组/路径优先于包含分组/路径；分组模式不带通配符时覆盖子分组；`api` 方法白名单与 opt-in（默认 `mcpTool`）；入参 schema（默认 `mcpInputSchema`，支持对象与 JSON 字符串，回退无参）。
- 依赖：无（工具桥与 datasource 主线解耦，可并行）。
- 约束：不引入 TTL 语义。
- 验收：注册表测试（含既有未提交用例）全部通过。

<!-- comet-task-ref:a8c96d4c-3811-4556-84e3-64475222d7c2 -->
### 7.2 ToolProvider 与 ToolExecutor

- 范围：工具名 `METHOD /path` 与函数名；同名时函数优先；参数原样透传；映射/集合按 JSON 序列化，其它按字符串；脚本异常与未知工具返回错误文本而不抛异常。
- 依赖：7.1。
- 约束：不把异常抛给模型调用循环；错误文本含异常摘要。
- 验收：Provider 与 Executor 测试全部通过。

<!-- comet-task-ref:678cf728-124b-4228-b6c0-731b21687191 -->
### 7.3 变更失效与装配开关

- 范围：资源变更事件使快照失效并按需重建；装配受 `fw.plus.magicapi.langchain4j.tool.enabled` 与 magic-api 资源服务可用性约束。
- 依赖：7.1。
- 约束：新增资源立即可见，无固定过期等待。
- 验收：变更监听测试与自动配置测试全部通过。

## 8. 集成验收与审计

<!-- comet-task-ref:a4873971-bad9-489b-9dfd-44ba509d5438 -->
### 8.1 集成测试

- 范围：上下文拉起后七类 storage/持有者/registry bean 齐备；以文件事件触发注册后按 `key` 可取、删除后消失；embedding-model → embedding-store 顺序场景。
- 依赖：1.3、2.1、4.3。
- 约束：扩展现有注册表断言风格，不新建并行测试基建。
- 验收：扩展后的注册测试全绿。

<!-- comet-task-ref:769f981a-99ac-4ff3-83a9-f508432d0623 -->
### 8.2 core 层零修改审计

- 范围：确认 `fw-plus-core` 无任何改动。
- 依赖：全部实现任务。
- 约束：不得通过修改 core 层绕过问题。
- 验收：`git diff --name-only -- fw-plus-core` 输出为空。

<!-- comet-task-ref:905777f1-c7d2-432e-ab03-88b2a42c4816 -->
### 8.3 样例静态配置不回归

- 范围：`fw-plus-ai-chat-sample` 现有 `langchain4j.open-ai.*` 路径不受影响。
- 依赖：3.1、2.1。
- 约束：不改动样例的静态配置语义。
- 验收：样例测试通过。

<!-- comet-task-ref:29014e55-455a-4757-8b19-2cd9f090c771 -->
### 8.4 命名审计

- 范围：folder、数据源类型、插件标识、包名、类名、脚本模块名均含 `langchain4j`，无 `uid` 残留。
- 依赖：2.1、6.2。
- 约束：审计覆盖后端与前端两侧。
- 验收：全仓检索无旧命名。

<!-- comet-task-ref:1d5dc780-2200-4d85-826c-2bc9be97a8f7 -->
### 8.5 控制台端到端验收

- 范围：七类数据源新建/保存/停用/删除/连接测试全链路可用，`options` 表单随 `provider` 变化。
- 依赖：6.2、5.1。
- 约束：需可复现的操作记录（所保存的 `{type}/*.json` 与测试返回）。
- 验收：手工操作记录齐备。
