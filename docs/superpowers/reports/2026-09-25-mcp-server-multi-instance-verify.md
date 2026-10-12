# 验证报告：mcp-server-multi-instance

日期：2026-09-25
阶段：verify（full）
验证模式：full（任务数 18 > 3，变更文件 > 8，多能力规格）

## 摘要

| 维度 | 状态 |
|------|------|
| Completeness | 任务 18/18 全部完成；能力规格 5 项 Requirement 全部覆盖 |
| Correctness | 核心场景/边界均验证通过；206 项单测仅 1 项已知无关基线失败 |
| Coherence | 实现与 design.md D1-D9 决策一致，delta spec 与 design doc 无矛盾 |

## 验证结果（full 模式 7 项）

| # | 检查项 | 结果 | 证据 |
|---|--------|------|------|
| 1 | tasks.md 全部任务已完成 | PASS | `tasks.md` 18/18 `[x]`（entry 校验 taskState completed: 18/18） |
| 2 | 实现符合 design.md 高层决策 | PASS | D1（registry 实例粒度）、D2（default+servers 继承模型）、D3/D9（编程式路由 + 双栈）、D4（runtime 工厂）、D5（认证占位）、D6（会话归属校验）、D7（@McpServer 归属）、D8（routeKey 广播）均落地 |
| 3 | 实现符合 Design Doc | PASS | `design.md` 即技术设计文档，实现与决策逐项一致；`docs/superpowers/specs/` 无独立重复文档需要对应（唯一 `2026-09-24-ai-chat-sample-design.md` 为其它 change） |
| 4 | 能力规格场景全部通过 | PASS | 见下方 Scenario 覆盖表 |
| 5 | proposal.md 目标已满足 | PASS | 多实例 runtime、注解归属、双栈、会话归属、routeKey 广播、顶层兼容单实例全部落地 |
| 6 | delta spec 与 design doc 无矛盾 | PASS | 本次无增量 spec 变更 |
| 7 | 关联设计文档可定位 | PASS | `docs/openspec/changes/mcp-server-multi-instance/design.md` 存在且与 change 相关 |

### Reliability 补充（合并前的真实保护）

| 检查 | 结果 | 证据 |
|------|------|------|
| 编译 | PASS | build check `1273e5f5` exit=0；autoconfigure/starter `install -DskipTests` exit=0 |
| 测试 | PASS | 样例集成 IT exit=0（verify check `6129b59d`）；autoconfigure 全量 206 项，唯一失败为已知无关基线 `LangChain4jAutoConfigurationRegistrationTest.shouldKeepEmbeddingStoreProviderSdksOutOfTheAutoConfigureCompileClasspath`（dev 分支既有，非本 change 引入） |
| 安全 | PASS | 无硬编码密钥；会话归属 404 防串台；WebFlux GET 缺头短路不再触注册表 NPE；异常路径解除流注册防内存泄漏 |

## Scenario 覆盖表（delta spec `mcp-server-starter`）

| Requirement | Scenarios | 覆盖证据 |
|-------------|-----------|----------|
| 多实例 runtime | servers 列表配置 / 顶层兼容单实例 / 同传输多实例 / 不同传输并存 / 同传输 endpoint 冲突 | `McpRuntimeFactoryTest`（11 项：default+servers 追加、继承、键冲突、endpoint 冲突、大小写冲突、双传输同 endpoint、transport 白名单、Redis/本地 registry 选择）、`McpServerSampleIT`（5 项端到端） |
| 编程式路由注册 | 动态路径注册 / SSE 与 Streamable 路径细节 | `McpRoutingRegistrarTest`、样例 IT（路径实测） |
| 双栈支持 | MVC 运行 / Flux 运行 / 归属校验一致性 / Flux SSE 出口 | `McpMvcSessionOwnershipContractTest`（5 项）、`McpFluxSessionOwnershipContractTest`（5 项：缺头 404 短路、未知会话 404、403 状态透出、200 SSE、异常关流）共享 `ContractRuntime` 构造 |
| 会话归属校验 | 跨 runtime 串台拒绝 / 合法会话放行 | 双栈契约测试全绿；样例 IT POST 归属 |
| 广播 routeKey | 多实例下广播不交叉 | Redis 集群 registry 实现 + 既有集群测试 |

## 集成代码审查

| 轮次 | 范围 | 结论 |
|------|------|------|
| 集成审查 1 | 全量 diff | CRITICAL 0 / IMPORTANT 2（WebFlux writer 身份、IT 路径）→ 已修复并复核确认 |
| 集成审查 2 | 全量 diff（修复后） | CRITICAL 0 / IMPORTANT 1（WebFlux GET 缺会话头 NPE）→ 已修复 |
| 定向复审 | 三处修复 | CRITICAL 0 / IMPORTANT 0；修复正确，新增测试真实有效 |
| 定向复审（WARNING 决策） | validateEndpoints 大小写、测试哨兵 | 通过；用户决策修复状态透出 + transport 白名单两项取舍 |
| 定向复审 | 状态透出 + 白名单修复 | CRITICAL 0 / IMPORTANT 1（openEventStream 异常关流）→ 已修复（verify_failures=4 用户选择继续） |
| 定向收官复审 | I1 修复 delta + 新断言测试 | CRITICAL 0 / IMPORTANT 0；三个出口（异常/非 200/取消）收敛到同 writer 幂等 close，同根因无残留 |

最终集成审查结论：**通过，无 CRITICAL / IMPORTANT**。

## 已接受且记录的 WARNING / SUGGESTION（不阻塞）

- WebFlux GET 事件流「连接断开 → doFinally 解除注册」仅由 mock 级契约测试覆盖，无端到端消除回归；且 mock 断言不校验 open/close 同一 writer 实例（后续可用 ArgumentCaptor 或真实 registry spy 加固）。
- WebFlux `synchronized(transport)` 与内核自身 synchronized 相比冗余——仅与 MVC 对齐保留，无正确性影响。
- `UnicastProcessor.create()` 队列无界，与旧 `Flux.create(BUFFER)` 行为相当，无回归。
- `validateEndpoints` 不校验 endpoint 空串（由路由注册器 `IllegalStateException` 兜底，既有低风险）。
- 异常关流仅捕获 `RuntimeException`，`Error` 不捕获（与 MVC `McpStreamableHttpController` 语义一致，可接受）。
- Redis cluster 静默丢弃、`findHandlerMapping` 静默跳过（本 change 既有设计，功能可用性记录）。
- 分组/双栈对外细节差异（非本 change 引入，不属范围）。

## 结论

无 CRITICAL / IMPORTANT 问题。全部 7 项验证通过，Bloom 上线风险已收敛。**可以归档**（归档需用户明确授权）。