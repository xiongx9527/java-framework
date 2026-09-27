---
generated_from_state_version: 13
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 2
- 验证器尝试次数: 1
- 完成时间: 2026-09-25T05:08:03.693Z
- 摘要: 迭代 2 修复了迭代 1 唯一未达成项 A3：空白实例 key 的报错由纯下标形态改为「fw.plus.ws.servers[].key must not be blank: fw.plus.ws.servers[N].key」，含 spec A22 要求的字面 fw.plus.ws.servers[].key，下标定位能力与重复 key 报文风格保留；改动仅 WsInstanceValidator 与 WsInstanceValidatorTest 两个文件（+4/-2）。runtime 侧独立复跑四项 check 全绿：ws 包 114 tests、magic-api ws 插件 22 tests、模块全量 752 tests 0 failures、docs 形态检查 exit 0。A1-A7 全部通过，可以进入归档确认。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | **A1｜Scenario**：配置形态 —— `fw.plus.ws.servers` 可按列表绑定，每项字段为 `key`、`path`、`paths`、`appId`；默认实例仍取顶层 `websocket.path`/`websocket.paths` 与 `appId`；未配 `servers` 时端点集合、会话 appId 与订阅派发和改造前完全一致。 | WsConfig.servers(List<WsInstance>) 与实例内 key/path/paths/appId 四字段绑定未变；默认实例端点仍由 WsEndpointPathResolver.resolveRelativePaths(WsConfig) 从 websocket.path/paths 播种，servers 为空时与两参重载等价；appId 仍由 AbstractWebSocketHandler 与 ReactiveWsHandler 共用 WsInstanceResolver.resolveAppId 解析。相对 iteration 1 候选仅有报文文案改动，绑定行为无回归；ws 包 15 类 114 tests 与全量 752 tests 复跑 0 failures。 |
| A2 | passed | brief.md | **A2｜Scenario**：旧键移除 —— YAML 写旧键 `fw.plus.ws.instances` 或实例内 `name` 时不再被绑定（Spring 默认忽略未知字段），不产生任何实例端点，默认实例行为不变，应用正常启动。 | 旧键 instances 与实例内 name 依旧无对应字段，legacyInstancesKeyShouldNotBind 断言 servers 空且 flatten 无 instances 扁平键；全仓 java/yml/md 无旧键残留（rg 只命中 cloud 层无关的 ServiceInstance），本次 2b30c7fd 只改 2 个文件、未触及绑定路径。 |
| A3 | passed | brief.md | **A3｜Scenario**：key 必填 —— 任一实例 `key` 缺失、空串或纯空白时启动失败，错误信息含 `fw.plus.ws.servers[].key`。 | 已修复：WsInstanceValidator.validateKey 空白 key 报文改为「fw.plus.ws.servers[].key must not be blank: fw.plus.ws.servers[N].key」（WsInstanceValidator.java:85），既含 spec A22 要求的字面 fw.plus.ws.servers[].key，又保留下标定位，且与同类重复 key 报文（fw.plus.ws.servers[].key '...' is declared more than once: ...）结构一致、与 McpRuntimeFactory:53 的非下标口径对齐。key 缺失、空串、纯空白三种情形均仍抛 IllegalArgumentException 并导致启动失败；WsInstanceValidatorTest.blankKeyShouldFail 已同步断言新文案，runtime 复跑 10 tests 0 failures，全仓无其他用例断言旧文案。 |
| A4 | passed | brief.md | **A4｜Scenario**：key 唯一 —— 两个实例 `key` 相同（trim 后）时启动失败，错误信息给出冲突的 key。 | trim 后重复 key 报文不变：fw.plus.ws.servers[].key 'report' is declared more than once: fw.plus.ws.servers[1].key conflicts with fw.plus.ws.servers[0].key，duplicateKeyShouldFail 保持通过；本次未改动该分支。 |
| A5 | passed | brief.md | **A5｜Scenario**：端点唯一 —— 实例端点与默认实例端点、或与另一实例端点（规范化后）重复时启动失败，错误信息含冲突路径与相关 key；同一实例内部 `path` 与 `paths` 重复仍按配置顺序去重、不失败；只配 `key` 不配任何端点同样启动失败。 | 端点冲突四类场景（与默认实例重复含尾斜杠与 websocket.paths 变体、实例间重复、只配 key 未配端点、实例内 path 与 paths 重复放行）行为与报文本轮未变，WsInstanceValidatorTest 覆盖并通过；全量回归 752 tests 0 failures 覆盖 webmvc 与 webflux 两条装配路径。 |
| A6 | passed | brief.md | **A6｜Scenario**：appId 继承与两栈一致 —— 实例未配 `appId`（留空）时握手会话 appId 取顶层 `fw.plus.ws.appId`，显式配置时取实例值；webmvc 与 webflux 两栈判定一致。 | webmvc AbstractWebSocketHandler:419 与 webflux ReactiveWsHandler:265 仍走 WsInstanceResolver.resolveAppId，命中且填 appId 取实例值、留空或未命中回落各自栈 defaultAppId（顶层 fw.plus.ws.appId）；WsHandshakeValidationTest 与 ReactiveWsHandlerTest 三种情形两栈一致，本轮复跑通过。 |
| A7 | passed | brief.md | **A7｜Scenario**：文档与样例同步 —— starter README 与样例 `application.yml` 只出现 `servers`/`key` 形态，无 `instances`/`name` 残留；ws 自动配置模块 ws 包测试改名后全绿（跳过项仅限依赖真实 Redis 的用例）。 | starter README、样例 application.yml、magic-api ws README 与 spec.md 未因本轮文案修复变动，仍只呈现 servers/key 形态；ws-docs-shape rg 检查 exit 0；ws 包 114 tests、magic-api ws 插件 3 类 22 tests、fw-plus-boot-autoconfigure 全量 147 类 752 tests 全部 0 failures，唯一 skip 是未设 Redis 环境变量的 WsRedisBroadcastEndToEndTest。 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| mvn -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=...web.ws.** (servers/key 绑定、WsInstanceResolver、WsInstanceValidator、webmvc+webflux 握手) | -o -q -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.** -DfailIfNoSpecifiedTests=false | fw-plus-boot | passed | 0 | 23923 ms |
| mvn -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=...magicapi.plugin.ws.** (magic-api ws 插件接线回归) | -o -q -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.** -DfailIfNoSpecifiedTests=false | fw-plus-boot | passed | 0 | 10810 ms |
| mvn -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test (fw-plus-boot-autoconfigure 全量回归) | -o -q -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test | fw-plus-boot | passed | 0 | 80596 ms |
| starter README / 样例 / magic-api ws README 只呈现 servers+key 形态且无旧键 instances 残留 | -c rg -q 'servers' fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ws-spring-boot-starter/README.md fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample && ! rg -n 'instances' fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ws-spring-boot-starter/README.md fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/README.md fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample | . | passed | 0 | 57 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- mvn -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.** -DfailIfNoSpecifiedTests=false: passed — WsInstanceValidatorTest 10 tests 0 failures，新文案断言通过
- mvn -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.** -DfailIfNoSpecifiedTests=false: passed — magic-api ws 插件 3 个类 22 tests 0 failures
- mvn -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test (全量回归): passed — 147 classes / 752 tests / 0 failures / 1 skipped，与 iteration 1 一致
- starter README / 样例 / magic-api ws README servers 形态且无旧键 instances 残留: passed — rg 检查 exit 0，文档未改动
- 已知限制: WsRedisBroadcastEndToEndTest 依赖真实 Redis，未设置环境变量时整类跳过；本次改动只涉及报文文案，未改变该已知限制。

## 阻塞项

_无。_

## 风险与跳过的工作

- 依赖真实 Redis 的 WsRedisBroadcastEndToEndTest 未设环境变量时整类跳过，跨实例广播端到端仍未由候选覆盖（iteration 1 既有已知限制，与文案修复无关）。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A3 | 候选整体扎实：servers/key 改名彻底、启动期校验在 webmvc 与 webflux 装配路径各执行一次、两栈 appId 回落一致、文档与样例同步、测试与全量回归全绿（ws 包 114 / magic-api ws 插件 22 / 模块 752，0 failures）。唯一未达成项是 A3 报错文案：空白 key 时给出 fw.plus.ws.servers[0].key must not be blank，不含字面 fw.plus.ws.servers[].key，与本 change 写入 spec 的 A22 和同仓 MCP 的非下标口径不符，需 Builder 调整后重新验收。 | 2026-09-25T04:59:07.634Z |
| 1 | 2 | 1 | pass | — | 迭代 2 修复了迭代 1 唯一未达成项 A3：空白实例 key 的报错由纯下标形态改为「fw.plus.ws.servers[].key must not be blank: fw.plus.ws.servers[N].key」，含 spec A22 要求的字面 fw.plus.ws.servers[].key，下标定位能力与重复 key 报文风格保留；改动仅 WsInstanceValidator 与 WsInstanceValidatorTest 两个文件（+4/-2）。runtime 侧独立复跑四项 check 全绿：ws 包 114 tests、magic-api ws 插件 22 tests、模块全量 752 tests 0 failures、docs 形态检查 exit 0。A1-A7 全部通过，可以进入归档确认。 | 2026-09-25T05:08:03.693Z |



## 结论

迭代 2 修复了迭代 1 唯一未达成项 A3：空白实例 key 的报错由纯下标形态改为「fw.plus.ws.servers[].key must not be blank: fw.plus.ws.servers[N].key」，含 spec A22 要求的字面 fw.plus.ws.servers[].key，下标定位能力与重复 key 报文风格保留；改动仅 WsInstanceValidator 与 WsInstanceValidatorTest 两个文件（+4/-2）。runtime 侧独立复跑四项 check 全绿：ws 包 114 tests、magic-api ws 插件 22 tests、模块全量 752 tests 0 failures、docs 形态检查 exit 0。A1-A7 全部通过，可以进入归档确认。
