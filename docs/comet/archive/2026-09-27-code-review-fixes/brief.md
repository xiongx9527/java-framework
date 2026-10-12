# Outcome

修复 code review（`a71713f3..HEAD`，10 个提交，281 文件）在两个轴上发现的问题：

- **Standards 轴 4 项硬违反**：boot MCP 模块删除后 pom 依赖残留（构建断裂）、AGENTS.md 模块树漏列与构建入口描述和 CLAUDE.md 矛盾、新 mcp 包 8 个主源与 ws 一处的 Tab/空格缩进不一致、两个 mcp 功能缺陷（控制台创建的 server 实例不进资源树、实例注销漏清增量状态）。
- **Spec 轴需求二 4 项未落地**：自定义 header 名称解析、capability scope 裁剪执行、tools 同名时 provider 优先、mcp 模块删除不彻底。
- **canonical ws spec 自洽滞后**：Scenario A1/A15 仍描述 `@Deprecated` 兼容委托与"flatten 新增 path、paths"，与 ws change 已确认决策（D1）矛盾。

ws 端点键提升本身的实现经 Spec 轴逐条复核**无偏离**，本次不改其行为。

# Scope

**硬缺陷（必做）**

1. **pom 依赖残留**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml` 的 dependencyManagement 仍管理已删除的 `fw-plus-magicapi-plugin-mcp`，删除该条目。
2. **文档同步**：AGENTS.md 模块树补列 HEAD 已存在的 `fw-plus-ai-mcp-server-sample`、`fw-plus-ai-chat-sample`；统一 AGENTS.md 与 CLAUDE.md 关于构建入口的描述（根 pom/mvnw 聚合 vs 无根 pom），以仓库实际结构与可执行命令为准，两份 AI 指南保持一致。
3. **缩进统一**：新 mcp 包 8 个用 4 空格的主源改为同包主导的 Tab（`MagicAPIMcpConfiguration.java`、`MagicApiMcpProperties.java`、`MagicMcpResourceInfoMagicResourceStorage.java`、`MagicMcpPromptInfoMagicResourceStorage.java`、`MagicApiMcpCapabilityProvider.java`、`resource/model/*.java`）；`fw-plus-boot/.../web/ws/common/WsBaseService.java:109-111` 插入的 Tab 改回空格（跟随该文件风格）。
4. **功能缺陷修复**：
   - `MagicDynamicMcpServer` 的 `serverInfos` 只 remove/read 从不 put，恒为空 → 控制台创建的实例不进 `serverInfos()` 资源树、javadoc 失真；补齐 put/生命周期管理并修正文档。
   - `MagicApiMcpCapabilityRegistrar.unregisterInstance` 漏清 `registeredTemplates` / `registeredCompletions`，实例重建后残留增量状态；补齐清理。
5. **canonical spec 自洽**：`docs/comet/specs/ws-spring-boot-starter/spec.md` Scenario A1（flatten "在改造前的基础上新增 path、paths"）与 A15（"旧拼写保留 `@Deprecated` 委托"）按 ws change 的 D1 决策重写：flatten 端点键集合为 `path`/`paths` 两个端点键 + 三个握手键、旧四键直接删除不留 `@Deprecated` 委托。

**spec 缺失补齐（Q1 已确认全部补齐）**

6. **自定义 header 名称解析**（`mcp-server-starter`）：starter 提供 header 名称配置（Accept、协议版本、会话 id、Origin 四类可按配置名称解析），缺省值保持当前内核默认行为；README 中 `headers.*` 引导的配置键必须真实存在。
7. **capability scope 裁剪执行**（`mcp-server-starter`）：入口消费 `McpPrincipal` 的 token scopes，对不含目标能力的请求按能力拒绝（tools/resources/prompts），缺省配置（未启用 scope 校验）时行为不变。
8. **tools 同名时 provider 优先**（`magicapi-mcp-plugin`）：`MagicApiMcpToolRegistry.buildSnapshot` 同名冲突时 provider 工具优先，与 resources/prompts 的 `putIfAbsent` 语义一致。

**判断调用类（Q2 已确认只修行为缺陷与死代码）**

9. `samePrompt()` 改为比对消息内容（改 prompt 文案触发刷新），与 `sameResource` 对称，补单测。
10. 删除 4 个无调用方方法（`MagicApiMcpToolRegistrar.resourceService()`、`MagicApiMcpServerRegistry.emptyArguments()`、`isRegistered()`、`newInstanceRegistrar()`）与 `MagicApiMcpToolRegistry.snapshot()` 上方无主 javadoc。

# Non-goals

- 样例 pom 与 `MagicApiAllPluginsClasspathTest`：其他会话正在主工作区修改同一处（把已删 boot mcp 依赖换成 java-entity/mybatisplus/mybatisflex/spring-bean + AI 层 mcp），本次排除以避免并发冲突。
- `fa3ac133` 已提交的 langchain4j pom 增补、`MagicAPIJavaEntityConfiguration` 注册前端插件、`.flattened-pom.xml` 生成物：属其他在途工作，不回退。
- ws change（`0abae46d`）的实现改动本身：Spec 轴复核无偏离。
- aj-mcp 内核的协议版本集合扩展（spec 举例 2025-11-25 而内核支持 2025-06-18/2025-03-26）：属上游内核范围，本次不动。
- Q2 未选中的结构性重构：四个 sync\* 方法与三个 `*MagicResourceStorage` 的共享形状提取、两个 Invoker 的参数重载组收敛、`MagicMcpClientGateway` capability 字符串 if 链改查找表、`MagicDynamicMcpServer` 三个同 key 并行 Map 合并、`findApi()` 全量扫改索引，均留待后续专口重构 change。
- `docs/agents/issue-tracker.md` 缺失（skills 安装问题，不属代码修复）。

# Acceptance examples

- **F1｜依赖清理**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml` 无 `fw-plus-magicapi-plugin-mcp` 条目；`cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure validate` 通过；全仓 grep `fw-plus-magicapi-plugin-mcp` 仅剩 AI 层 `fw-plus-ai-magicapi-plugin-mcp` 与 docs 归档。
- **F2｜文档一致**：AGENTS.md 模块树包含 `fw-plus-ai-mcp-server-sample` 与 `fw-plus-ai-chat-sample`；AGENTS.md 与 CLAUDE.md 对构建入口的描述不矛盾，且描述的命令与实际仓库结构相符。
- **F3｜缩进**：清单内 9 个文件与同包/同文件主导缩进风格一致（脚本化检查：目标行首为 Tab 或空格的判定）。
- **F4｜serverInfos 生命周期**：单测断言控制台创建的 server 实例出现在 `serverInfos()` 结果中，unregister 后消失；`MagicMcpServerMagicDynamicRegistry.defaultMappings()` 覆盖到该实例。
- **F5｜实例注销清理**：单测断言 `unregisterInstance` 后 `registeredTemplates` / `registeredCompletions` 中该实例的登记项被清空，重新注册同一实例不残留旧状态。
- **F6｜canonical spec 自洽**：Scenario A1 描述 flatten 端点键为 `path`/`paths`（改造后形态），A15 不再出现 `@Deprecated` 委托表述；spec 与 `WsConfig.flatten()` 实际输出、ws change 的 D1 一致。
- **F7｜header 名称配置**：starter 提供 Accept/协议版本/会话 id/Origin 的 header 名称配置键，缺省值与当前内核默认一致；单测覆盖自定义 header 名生效与缺省回落两条路径。
- **F8｜scope 裁剪执行**：入口按 `McpPrincipal` 的 scopes 拒绝不含目标能力的请求（tools/resources/prompts），缺省配置下行为不变；单测覆盖拒绝与放行两条路径。
- **F9｜tools provider 优先**：`MagicApiMcpToolRegistry.buildSnapshot` 同名冲突时 provider 工具优先，单测覆盖动态资源占名场景。
- **F10｜samePrompt 与死代码**：`samePrompt()` 比对消息内容（改文案触发刷新）并有单测；被删除的 4 个方法与无主 javadoc 在全仓无调用方与残留。

# Constraints and invariants

- JDK 1.8、UTF-8、简体中文注释；缩进跟随周边文件（mcp 包主导：主源 Tab，测试类空格）。
- 不改 MCP 协议线上行为，补实现时缺省配置必须保持现有行为（向后兼容）：header 缺省名 = 现内核默认、缺省不启用 scope 拒绝。
- 分层不破坏：AI 层插件不反向依赖 Boot 层已删模块。
- 提交只包含本 change 相关路径，不夹带主工作区其他会话的在途改动。
- canonical spec 只改 A1/A15 两处滞后描述，不顺手改其他已验收场景。

# Decisions

- **D1 样例 pom 排除**：其他会话正在修改同一文件同一行区域，避免并发冲突；该处残留由其修复覆盖，本 change 的 F1 以 boot-dependencies 为准。
- **D2 canonical spec 以实现为准**：ws change 的 D1（不留 `@Deprecated`）经用户确认并已归档，spec 文本滞后；更新文本而非回退实现。
- **D3 spec 缺失全部补齐（Q1  answered=全部补齐）**：header 名称、scope 裁剪、tools provider 优先三项均在本 change 实现并配单测；缺省值保持现行为以兼容存量部署。
- **D4 判断调用类只修行为缺陷与死代码（Q2  answered=只修行为缺陷与死代码）**：`samePrompt()` 不对称是实际行为缺陷故修复；重复代码/if 链/并行 Map/findApi 等结构性重构不做，留待后续专口 change。

# Open questions

（无）

# Verification expectations

- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.mcp.**.*Test'` 全绿（覆盖 F4/F5/F7/F8/F9/F10 与相关新用例）。
- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test` 模块全量回归通过。
- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-dependencies validate` 通过（F1）。
- 缩进脚本检查 F3 清单文件通过。
