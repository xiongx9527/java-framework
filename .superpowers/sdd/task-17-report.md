# Task 17 聚合验证报告

## STATUS: DONE

## TDD 说明

本任务以聚合验证和状态更新为主。初次 GREEN 发现 AI 自动配置定向测试存在 1 项失败，随后按 TDD/调试流程复现 RED，并通过测试夹具最小修复提交 `19e39cd1`。最终 Core 测试、AI 自动配置 AskData 测试、Starter 打包和文档占位符检查均符合预期。

## 验证结果

| 命令 | 退出码 | 关键结果 |
| --- | ---: | --- |
| `cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-askdata test` | 0 | `BUILD SUCCESS`；52 项测试通过，0 failures、0 errors、0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test` | 0 | 初次运行 112 项中 1 项失败；修复提交 `19e39cd1` 后复跑 112 项通过，0 failures、0 errors、0 skipped。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -DskipTests package` | 0 | `BUILD SUCCESS`；测试按参数跳过。Maven 同时报告本地 `fw-plus-ai-boot-autoconfigure:0.1.1-SNAPSHOT` POM 无效警告，Starter 仍成功打包。 |
| `rg -n "T[B]D|T[O]DO|待[补]|占[位]" docs/askdata/starter docs/superpowers/specs/2026-07-27-rework-ai-askdata-core-starter-design.md` | 1 | 无输出；`rg` 以 1 表示未匹配，符合文档检查预期。 |

## 失败定位与修复

失败可稳定复现。该测试仅提供 Core 会话、检查点、Agent 状态、向量检索和旧会话 Store；它未提供 `AskDataLongTermMemory` 或 `AskDataRecommendationService`。集群模式下，`AskDataAutoConfiguration#askDataLongTermMemory` 会先调用 `validateLocalStateConfiguration`，因此启动失败消息为默认本地内存状态，而不是测试断言要求的“监控日志”。

根因位于测试装配前提不完整，而不是生产自动配置必须改变。修复已在 `AskDataMonitoringLogStoreTest` 中补齐长期记忆和推荐 SPI 夹具，使该测试继续故意缺失 `AskDataMonitoringLogStore`，从而精确覆盖监控日志集群拒绝分支。

## OpenSpec 状态

已勾选：1.7；7.0、7.1、7.2、7.3、7.4、7.5、7.6、7.7、7.8、7.9、7.10、7.11、7.12、7.13。

依据：Core 定向测试 52 项全绿；AI 自动配置 AskData 回归修复后 112 项全绿；Task 15/16 已审查通过的 Starter 文档、SQLBot/WrenAI 矩阵和测试用例文档均存在，且本次占位符扫描无输出。未勾选 Starter 集成、共享存储、外部导出及后续运行时能力，因为缺少完整实现或不属于本批次可验证范围。

计划 Task 17：Step 1、Step 2、Step 3、Step 4、Step 5 已实际完成；Step 6 将由主会话提交本报告、OpenSpec 状态和计划进度后完成。

## 修改文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogStoreTest.java`
- `openspec/changes/rework-ai-askdata-core-starter/tasks.md`
- `.superpowers/sdd/task-17-report.md`

## 提交

- 修复提交：`19e39cd1ace32af9ac32665e36535f55a33fed2a`（`test: isolate askdata monitoring log cluster check`）。
- 验证状态提交由主会话创建。

## 风险信号

- 公共流程/验证风险：本次测试通过 Bean 存在性验证自动配置让位关系，不模拟真实分布式存储的跨进程语义；该语义属于具体业务 SPI 实现的集成验证范围。
- 构建风险：Starter 打包虽通过，但本地 Maven 报告 `fw-plus-ai-boot-autoconfigure:0.1.1-SNAPSHOT` POM 无效警告。

---

## Task 17 修复记录（2026-07-28）

## STATUS: RESOLVED

### 根因

`AskDataMonitoringLogStoreTest#shouldRejectDefaultMonitoringLogStoreInClusterModeWhenBusinessProvidesOtherSharedStores`
仅替换了 Core 会话、检查点、Agent 状态、向量和旧会话 SPI，漏掉同样受集群模式默认内存校验的
`AskDataLongTermMemory` 与 `AskDataRecommendationService`；上下文因此先在 `askDataLongTermMemory` 失败，
无法到达目标监控日志校验。

### RED

| 命令 | 退出码 | 关键结果 |
| --- | ---: | --- |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test` | 1 | 112 项中 1 项失败；失败 Bean 为 `askDataLongTermMemory`，消息为“默认本地内存状态”，断言未包含“监控日志”。 |

### GREEN

| 命令 | 退出码 | 关键结果 |
| --- | ---: | --- |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataMonitoringLogStoreTest test` | 0 | 3 项通过；集群启动失败精确落在 `askDataMonitoringLogStore`，消息包含“默认本地监控日志”。 |
| `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test` | 0 | 112 项通过，0 failures、0 errors、0 skipped。 |

### 修改文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/observability/AskDataMonitoringLogStoreTest.java`
- `.superpowers/sdd/task-17-report.md`

### 提交

- 修复提交：`19e39cd1ace32af9ac32665e36535f55a33fed2a`（`test: isolate askdata monitoring log cluster check`）

### 风险信号

- 本测试只验证自动配置会让位于业务提供的 SPI；共享存储实现的跨节点语义仍由具体业务实现及其集成测试保证。
- Maven 输出仍包含既有的多个 SLF4J binding 和 `EmbeddingStoreAutoConfigurationTest` 未检查操作告警；本次未修改这些范围外问题。

## Blocker Fix（2026-07-28）

### 根因和最小修复

`shouldRejectDefaultMonitoringLogStoreInClusterModeWhenBusinessProvidesOtherSharedStores` 的夹具缺少
`AskDataLongTermMemory` 和 `AskDataRecommendationService`。在集群模式下，自动配置会先创建并拒绝默认
`AskDataLongTermMemory`，因此原断言无法到达缺失 `AskDataMonitoringLogStore` 的目标分支。

测试夹具现显式提供这两个业务 SPI Bean，并继续故意不提供 `AskDataMonitoringLogStore`。未修改生产源码、POM、
其他测试或 OpenSpec 任务清单。

### TDD 验证证据

| 阶段 | 命令 | 退出码 | 结果 |
| --- | --- | ---: | --- |
| RED | `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=AskDataMonitoringLogStoreTest#shouldRejectDefaultMonitoringLogStoreInClusterModeWhenBusinessProvidesOtherSharedStores test` | 1 | 临时移除上述两个 SPI 夹具后，启动首先在 `askDataLongTermMemory` 失败，消息为“默认本地内存状态”，监控日志断言失败。 |
| GREEN | 同上 | 0 | 1 项通过；启动失败已落在 `askDataMonitoringLogStore`，消息包含“默认本地监控日志”。 |
| GREEN 回归 | `cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest='*AskData*Test' test` | 0 | 112 项通过，0 failures、0 errors、0 skipped。 |

### 剩余关注项

- 测试通过 Bean 存在性验证自动配置让位关系，不模拟真实分布式存储的跨进程语义；该语义属于具体业务 SPI 实现的集成验证范围。
- 本轮 Maven 仍输出既有的多 SLF4J 绑定、Jansi restricted method 和 `Unsafe` 警告，但不影响两次 GREEN 结果。
