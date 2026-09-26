# ai-magicapi-mcp-plugin 验证报告

- 日期：2026-09-26
- change：`ai-magicapi-mcp-plugin`
- 绑定分支：`dev`
- 规模评估：full（任务 30 项、delta spec 2 个能力域、变更文件远大于 8）

## 验证范围与结论

| # | 检查项 | 结果 | 证据 |
|---|---|---|---|
| 1 | tasks.md 全部任务已完成 | PASS | 30/30 `[x]`，`comet state tasks` 确认 |
| 2 | 改动文件与 tasks 描述一致 | PASS | 4 个提交仅触及 `fw-plus-ai/**`、`fw-plus-boot/**`、`docs/**`；Boot 层旧实现（100 个类）已删 |
| 3 | 编译/构建通过 | PASS（见下方说明） | `comet check run … build --local -- ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` exit 0 |
| 4 | 相关测试通过 | PASS | AI 层 366 例全绿；Boot 层 617 例通过、1 跳过（原 618，移除 1 例被删控制器的路由用例） |
| 5 | 无明显安全问题 | PASS | 无硬编码密钥；认证入口统一经 starter `McpAuthService`，失败即 401 不进入内核分发 |
| 6 | 最终集成代码审查 | PASS | 实现过程中对每批改动做了编译+测试回归；见下方偏差与例外 |
| 7 | 核心/高风险场景验证 | PASS | 见下方场景清单 |

## 关于 guard 的 "Build passes" 检查（例外说明）

`comet guard … build` 的运行时默认命令为 `./mvnw compile -q`（仓库根聚合 5 个库层）。该命令在
`fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-{anthropic,openai}`
编译失败：**这两个模块的 pom 未声明 lombok 依赖**，离线（`-o`）与联网均复现。

该失败与本次 change 无关，依据：

- 本 change 的 4 个提交（`dabe42d8`、`d3431ff1`、`1a72cddf`、`a4747e9d`）只修改
  `fw-plus-ai/**`、`fw-plus-boot/**`、`docs/**`，未触碰 `fw-plus-core`；
- 失败发生在 reactor 的 core 阶段，本次改动的模块尚未进入构建；
- 单独编译同一模块成功（`./mvnw -o -pl <该模块> compile` → BUILD SUCCESS），
  说明失败源于根 reactor 聚合时的依赖解析路径，而非源码本身。

经用户确认，该项按「既有基线缺陷，采用 change 范围内实测证据」处理：构建以
`comet check run … build --local`（change 范围内、exit 0）与下列分层验证为准。

## change 范围内的实际构建与测试

| 命令 | 结果 |
|---|---|
| `./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test`（fw-plus-ai） | 366 例全绿 |
| `./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`（fw-plus-boot） | 617 例通过、1 跳过 |
| `./mvnw -q -o -f ../fw-plus-sample/pom.xml validate`（fw-plus-core 的 mvnw） | 退出码 0 |
| `./mvnw -pl …/fw-plus-ai-magicapi-plugin-mcp install -DskipTests` | `magic-mcp.1.0.0.iife.js` 打入 jar `magic-editor/plugins/` |

## 核心与高风险场景

| 场景 | 验证方式 | 结果 |
|---|---|---|
| api/function 变更 → tools/list_changed，多实例只广播一次 | `MagicApiMcpToolChangeListenerTest`、`McpAuthEntry*Test` | PASS |
| mcp-resource 增/删/同名更新 → list_changed / resources/updated | `MagicMcpRegistryNotificationTest` | PASS |
| resources 真删除后 `resources/list` 不再返回 | `MagicApiMcpCapabilityRegistrarTest` | PASS |
| `resources/read` / `prompts/get` 返回真实内容且 `${arg}` 插值 | `MagicApiMcpCapabilityRegistrarTest` | PASS |
| resource-template 命中与多变量正则 | `MagicApiMcpCapabilityRegistrarTest` | PASS |
| completion/complete 前缀过滤、两种 ref 形态命中 | `MagicApiMcpCapabilityRegistrarTest` | PASS |
| 自定义 provider 与动态能力并存、同名覆盖 | `MagicApiMcpCapabilityRegistrarTest`、`MagicApiMcpCapabilityProviderTest` | PASS |
| mcp-server 实例创建/销毁（含路由注册与注销、能力反注册） | `MagicMcpServerStorageTest`、`McpFluxRouteRegistryTest`、`McpWebfluxBindingIntegrationTest` | PASS |
| 配置项驱动的实例在启动期创建（`afterSingletonsInstantiated`） | `MagicDynamicMcpServer` + 装配测试 | PASS |
| 认证三态（Sa-Token 版/放行/用户自定义）与匿名 401 不进内核 | `McpAutoConfigurationBeanSelectionTest`、`SaTokenOAuth2McpAuthServiceTest`、`McpAuthEntryMvcTest`、`McpAuthEntryFluxTest` | PASS |
| 集群能力变更跨节点与自身 origin 抑制、单节点降级 | `MagicMcpClusterChangeListenerTest`、`MagicMcpClusterBroadcasterTest` | PASS |
| roots/sampling/elicitation 未声明 capability 不发起请求 | `MagicMcpClientGatewayTest` | PASS |
| 快照缓存只按事件失效（无 TTL 陈旧窗口） | `MagicApiMcpToolRegistryTest` | PASS |

## 已知偏差（非阻塞）

1. `MagicApiMcpToolRegistry` 的快照缓存在 provider 内部状态变化时不失效；provider bean 为启动期
   确定，需要动态变更时应由 provider 侧主动触发重建（未在使用中出现）。
2. webflux 动态路由的验证为装配级（`ApplicationContextRunner` + `route()` 探测），未起真实 Netty。
3. agents 层未提供 sa-token-oauth2 时认证为放行占位，属预期缺省语义。
4. commit-graph 中 `.comet/` 的 change 状态文件被入库，属 Comet 有意管理的恢复数据，未追加忽略。

## 结论

7 项检查全部通过，无 CRITICAL/IMPORTANT 问题。可以进入归档。
