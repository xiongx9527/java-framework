---
generated_from_state_version: 27
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 6
- 验证器尝试次数: 1
- 完成时间: 2026-09-27T06:11:49.151Z
- 摘要: 10 项全部通过。A2 本轮闭环：CLAUDE.md 构建命令块注释与模块树头部行统一为根聚合口径，三处构建入口（模块树头部/构建命令块/构建顺序段）与 AGENTS.md 逐条自洽且与仓库实际结构相符，旧表述 grep 零残留。A1 的 validate 由 Verifier 亲跑 BUILD SUCCESS；样例两测试 6/6、A4-A10 相关 AI 层 MCP 测试 97+ 例 surefire 全绿；A6/A7 对 canonical spec 与 aj-mcp 1.6 内核常量逐字核对无偏离。遗留均为非阻塞（brief 模块路径笔误、两栈状态码不对称、CLAUDE.md 样例树预存不完整）。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | **F1｜依赖清理**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml` 无 `fw-plus-magicapi-plugin-mcp` 条目；`cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure validate` 通过；全仓 grep `fw-plus-magicapi-plugin-mcp` 仅剩 AI 层 `fw-plus-ai-magicapi-plugin-mcp` 与 docs 归档。 | boot-dependencies 无 fw-plus-magicapi-plugin-mcp；全仓 *.xml grep 零命中（非 xml 命中仅 AI 层 README 迁移说明与 docs 归档，符合豁免）；样例 31 插件坐标 == 磁盘聚合模块（boot 28/ai 3，测试 assertEquals 自校验）；样例两测试 surefire 6/6；A1 原文 validate 由 Verifier 亲跑 BUILD SUCCESS。 |
| A2 | passed | brief.md | **F2｜文档一致**：AGENTS.md 模块树包含 `fw-plus-ai-mcp-server-sample` 与 `fw-plus-ai-chat-sample`；AGENTS.md 与 CLAUDE.md 对构建入口的描述不矛盾，且描述的命令与实际仓库结构相符。 | AGENTS.md:162-163 含两个 AI sample 且目录存在；三处构建入口逐条自洽：模块树头部 CLAUDE.md:27 与 AGENTS.md:32 逐字一致、构建命令块 CLAUDE.md:401-407 与 AGENTS.md:295-296 同为根聚合口径、构建顺序段 CLAUDE.md:415 与 AGENTS.md:282 完全一致；与仓库实际相符（根 pom 恰聚合 5 模块、根 mvnw 存在、四层各有 mvnw、sample 无 mvnw 有 pom）；旧表述 grep 零残留。 |
| A3 | passed | brief.md | **F3｜缩进**：清单内 9 个文件与同包/同文件主导缩进风格一致（脚本化检查：目标行首为 Tab 或空格的判定）。 | 8 个 mcp 主源 Tab 行压倒性主导、零处非 javadoc 空格缩进代码行；WsBaseService 全文件 0 个前导 Tab 行（333 空格缩进行）。 |
| A4 | passed | brief.md | **F4｜serverInfos 生命周期**：单测断言控制台创建的 server 实例出现在 `serverInfos()` 结果中，unregister 后消失；`MagicMcpServerMagicDynamicRegistry.defaultMappings()` 覆盖到该实例。 | MagicDynamicMcpServer serverInfos 的 put(:118-120)/remove(:137)/读(:165) 闭环；MagicMcpServerMagicDynamicRegistry.java:67 defaultMappings() 直接委托；MagicDynamicMcpServerTest 3/3 绿。 |
| A5 | passed | brief.md | **F5｜实例注销清理**：单测断言 `unregisterInstance` 后 `registeredTemplates` / `registeredCompletions` 中该实例的登记项被清空，重新注册同一实例不残留旧状态。 | unregisterInstance(:98) 补 registeredTemplates.remove(:105)/registeredCompletions.remove(:106) 并清四 store；专项测试断言四表清空与重建无残留；RegistrarTest 17/17 绿。 |
| A6 | passed | brief.md | **F6｜canonical spec 自洽**：Scenario A1 描述 flatten 端点键为 `path`/`paths`（改造后形态），A15 不再出现 `@Deprecated` 委托表述；spec 与 `WsConfig.flatten()` 实际输出、ws change 的 D1 一致。 | canonical spec Scenario A1(:126)/A15(:140) 与 WsConfig.flatten() 实际只 put path/paths+三 websocket 握手键一致；WsConfig 全文件无 @Deprecated；Websocket 仅剩三个握手字段。 |
| A7 | passed | brief.md | **F7｜header 名称配置**：starter 提供 Accept/协议版本/会话 id/Origin 的 header 名称配置键，缺省值与当前内核默认一致；单测覆盖自定义 header 名生效与缺省回落两条路径。 | McpProperties.Headers 四键 + McpHeaderNames 缺省常量经 javap 与 aj-mcp 1.6 内核逐字相符（MCP-Protocol-Version/Mcp-Session-Id）；两栈接入；README:129-132 四键文档化；McpHeaderNamesTest 5/5 绿。 |
| A8 | passed | brief.md | **F8｜scope 裁剪执行**：入口按 `McpPrincipal` 的 scopes 拒绝不含目标能力的请求（tools/resources/prompts），缺省配置下行为不变；单测覆盖拒绝与放行两条路径。 | 入口按 McpCapabilities.covers(principal, ofBody(body)) 裁剪；webmvc 403 / webflux 401；webflux post/handleMessage 与 webmvc 均先读 body 再判定，legacy SSE body 空转已闭环；McpScopeEntryMvcTest 4/4 + FluxTest 5/5 覆盖拒绝/放行/缺省/非能力操作。 |
| A9 | passed | brief.md | **F9｜tools provider 优先**：`MagicApiMcpToolRegistry.buildSnapshot` 同名冲突时 provider 工具优先，单测覆盖动态资源占名场景。 | buildSnapshot 经 addByName 后注册同名 result.set 覆盖，provider 循环在动态能力之后故同名 provider 优先；同名 echo 被 provider 覆盖的专项断言在列；ToolRegistryTest 27/27 绿。 |
| A10 | passed | brief.md | **F10｜samePrompt 与死代码**：`samePrompt()` 比对消息内容（改文案触发刷新）并有单测；被删除的 4 个方法与无主 javadoc 在全仓无调用方与残留。 | samePrompt(:397) 除 prompt JSON 外再比 readMessages 消息内容，改文案触发替换有双路测试覆盖；4 个死方法全仓零定义零调用；snapshot() javadoc 有主且与行为相符，无无主 javadoc 残留。 |

## 检查

_没有记录 Runtime 检查。_

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- 文档一致性核对（grep 无残留旧表述）: passed — grep -n '根目录无统一 POM|需在各模块目录下执行' CLAUDE.md AGENTS.md 均零命中；CLAUDE.md:27/401 与 AGENTS.md:32/282/296 措辞逐条对应
- 已知限制: mybatisplus/mybatisflex starter 依赖 optional 化对存量宿主是行为变更（README 已补迁移说明）。
- 已知限制: 样例 all-plugins profile 以 fw.plus.mp.enabled=false + 排除 Redis 适配离线环境。
- 已知限制: scope 拒绝状态码两栈不同：webmvc 403、webflux 401。
- 已知限制: 样例 pom 与两个 classpath 测试同主工作区并行修改区域重叠，合入 dev 需人工解冲突。

## 阻塞项

_无。_

## 风险与跳过的工作

- brief「Verification expectations」把 MCP 测试指向 fw-plus-boot-autoconfigure，与 MCP 已迁至 fw-plus-ai-boot-autoconfigure 的现状不符（实际执行模块为 AI 层、均绿；不影响验收项）。
- A8 两栈 scope 拒绝状态码不一致（webmvc 403 / webflux 401）：已披露限制，符合 F8 字面（要求发生拒绝而非状态码一致）。
- CLAUDE.md 模块树 AI-samples 段（:155-156）仍只列 fw-plus-ai-magicapi-sample 一个子项，未列已存在的 mcp-server-sample/chat-sample；A2 的补列要求仅针对 AGENTS.md（已满足），此为样例树完整性问题非构建入口矛盾，建议后续文档 pass 补齐。
- mybatisplus/mybatisflex starter 依赖 optional 化对存量宿主是行为变更（README 已补说明）；样例 all-plugins 以 mp 关闭 + Redis 排除适配离线；样例 pom 与 classpath 测试同主工作区并行修改，合 dev 需人工解冲突。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A8 | 10 项中 9 项通过（依赖清理、文档一致、缩进、serverInfos 生命周期、unregisterInstance 增量表清理、canonical spec 自洽、header 配置两栈接入、tools provider 优先、samePrompt 与死代码清除），均有代码+单测+260/385 绿色回执支撑。A8 失败：scope 裁剪未覆盖 webflux 的 legacy SSE 消息入口（http-sse 为 starter 默认传输），该路径 body 恒为 null 导致裁剪空转，违反 Scenario S2 与两栈一致不变量。建议回 Build 修补 handleMessage 的 body 透传并补 webflux 入口级单测，顺带补 A1 原文指定的 -autoconfigure validate、清理 AGENTS.md/CLAUDE.md 已删模块条目、把两个新配置键写入 starter README。 | 2026-09-27T03:53:39.316Z |
| 1 | 2 | 1 | fail | A1 | 9 项（A2-A10）通过，A8 的 webflux legacy SSE scope 空转已确认修复（授权后移+透传 body，两栈 9 例断言逐条对应）。唯一失败 A1：样例两个测试期望集合与磁盘不符（classpath 24 vs 实际 28 模块；ApplicationContextTest mapped 26 vs declared 27 差 AI mcp 条目），且 A1 原文指定的 autoconfigure validate 未执行。另 README 新配置键未提交、dev 有同区域平行修改。建议回 Build：对齐磁盘 28 模块清单与数量断言、补 AI 侧 mcp 映射、补跑 validate、提交 README 后重验。 | 2026-09-27T04:28:45.745Z |
| 1 | 3 | 1 | fail | A1 | 9 项（A2-A10）通过。A1 失败：其官方三项与 ClasspathTest 均已满足，但 ApplicationContextTest 当前失败由本 change 引入——样例 pom 新增 fw-plus-magicapi-plugin-mybatisplus 使 mybatis-plus-boot-starter 进入 classpath、激活 MpAutoConfiguration→MpBaseAutoConfiguration，而 fw-plus-mp-base（autoconfigure optional）不在样例 classpath，报 NoClassDefFoundError: MpSqlInjector；Builder 交接的既有 resultProvider 归因被 surefire 证据否证（该冲突本身早于基线属实但非当前失败模式）。建议回 Build：为样例补齐 fw-plus-mp-base（或 fw-plus-mp-spring-boot-starter）使链类可达，并复跑两个样例测试全量。 | 2026-09-27T04:54:44.685Z |
| 1 | 4 | 1 | pass | — | 10 个验收项全部基于磁盘代码、单测与 Runtime 回执逐项独立核查通过。A1：BOM 与样例 pom 三方对账（boot 28/ai 3）一致、样例两测试 6/6 全绿、grep 零残留，optional 化判定为可接受的产品侧修复；A2：文档一致且与根聚合结构相符；A3：缩进脚本化通过；A4/A5：生命周期与增量清理有实现加测试支撑；A6：canonical spec 与 flatten 实际输出一致；A7：header 配置缺省名与内核常量相符、两栈接入、README 文档化；A8：前三轮失败点确认修复，两栈 9 例入口级 + 6 例判定单测全绿；A9/A10：provider 优先与 samePrompt/死代码清理均有实现与单测支撑。主要遗留：实现提交误入库 874KB 测试日志及其持续工作区污染（不属验收范围），建议合入前 git rm --cached 闭环。 | 2026-09-27T05:30:37.440Z |
| 1 | 4 | 1 | recovery | — | 用户选择继续修改实现：补齐 Verifier 建议的两个 mybatis 插件 README 迁移说明（starter 依赖 optional 化对存量宿主是真实行为变更，属 Verifier 明确指出的文档缺口），顺带按 Verifier 建议用补充提交把误入库的样例测试日志 git rm --cached 闭环。已确认的需求范围与验收标准不变。 | 2026-09-27T05:31:48.485Z |
| 1 | 5 | 1 | fail | A2 | 9 项通过。唯一失败 A2：CLAUDE.md 构建命令块注释（:401）仍写「需在各模块目录下执行，根目录无统一 POM」，与 :415、AGENTS.md:282/296 及仓库实际（根 pom.xml 聚合 5 模块 + mvnw）矛盾；brief 硬缺陷 #2 只改正文段遗漏命令块。应回 Build 同步该命令块后重验。 | 2026-09-27T05:58:53.314Z |
| 1 | 6 | 1 | pass | — | 10 项全部通过。A2 本轮闭环：CLAUDE.md 构建命令块注释与模块树头部行统一为根聚合口径，三处构建入口（模块树头部/构建命令块/构建顺序段）与 AGENTS.md 逐条自洽且与仓库实际结构相符，旧表述 grep 零残留。A1 的 validate 由 Verifier 亲跑 BUILD SUCCESS；样例两测试 6/6、A4-A10 相关 AI 层 MCP 测试 97+ 例 surefire 全绿；A6/A7 对 canonical spec 与 aj-mcp 1.6 内核常量逐字核对无偏离。遗留均为非阻塞（brief 模块路径笔误、两栈状态码不对称、CLAUDE.md 样例树预存不完整）。 | 2026-09-27T06:11:49.151Z |



## 结论

10 项全部通过。A2 本轮闭环：CLAUDE.md 构建命令块注释与模块树头部行统一为根聚合口径，三处构建入口（模块树头部/构建命令块/构建顺序段）与 AGENTS.md 逐条自洽且与仓库实际结构相符，旧表述 grep 零残留。A1 的 validate 由 Verifier 亲跑 BUILD SUCCESS；样例两测试 6/6、A4-A10 相关 AI 层 MCP 测试 97+ 例 surefire 全绿；A6/A7 对 canonical spec 与 aj-mcp 1.6 内核常量逐字核对无偏离。遗留均为非阻塞（brief 模块路径笔误、两栈状态码不对称、CLAUDE.md 样例树预存不完整）。
