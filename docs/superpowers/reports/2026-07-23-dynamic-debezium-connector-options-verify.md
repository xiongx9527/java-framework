# 验证报告：dynamic-debezium-connector-options

## 概览

| 维度 | 状态 |
|------|------|
| 完整性 | OpenSpec 任务 12/12 已完成；实施计划步骤 19/19 已完成 |
| 正确性 | 3/3 个需求已覆盖；6/6 个场景有实现证据 |
| 一致性 | 符合 proposal、OpenSpec design 和技术设计文档 |

## 验证模式

完整验证。`comet state scale dynamic-debezium-connector-options` 因任务数为 12，选择 `full` 验证模式。

## 验证证据

| 检查项 | 命令 | 结果 |
|--------|------|------|
| Verify 入口检查 | `comet state check dynamic-debezium-connector-options verify` | 通过 |
| OpenSpec 任务状态 | `openspec instructions apply --change dynamic-debezium-connector-options --json` | 12 total, 12 complete |
| 后端定向测试 | `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDebeziumControllerTest test` | 通过，3 个测试 |
| Debezium 插件完整构建 | `cd fw-plus-boot && mvn clean install -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium -DskipTests` | 通过 |
| 控制台构建 | `cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console && npm run build` | 通过 |
| OpenSpec 校验 | `openspec validate dynamic-debezium-connector-options` | 通过 |
| 空白检查 | `git diff --check` | 通过 |
| Build 阶段守卫 | `comet guard dynamic-debezium-connector-options build --apply` | 通过，phase 已推进到 verify |
| 代码审查 | standard reviewer subagent | 未发现 Critical 或 Important 问题 |

`fw-plus-boot/./mvnw` 无法启动，原因是缺少 `.mvn/wrapper/maven-wrapper.properties` 和 `.mvn/wrapper/maven-wrapper.jar`。本轮已使用系统 Maven 3.6.3 完成等价验证。

## 需求覆盖

### Dynamic connector option loading

已覆盖。

- 后端新增 `MagicDebeziumController.connectorOptions()`，返回 `JsonBean<List<DebeziumConnectorOption>>`。
- 前端服务新增 `loadConnectorOptions(success, failure)`，调用 `/debezium/datasource/connector-options`。
- Vue 表单使用 `connectorOptions = ref([])`，并在 `onMounted()` 中调用 `loadConnectorOptions()`。
- Vue 组件中原五项硬编码 `connectorOptions` 数组已移除。

场景覆盖：

- 打开数据源表单：由 `onMounted()` 服务调用和 `magic-select :options="connectorOptions"` 覆盖。
- 后端支持范围变化：组件渲染后端响应，不再依赖 Vue 源码内固定列表。

### Default connector compatibility

已覆盖。

- `DEFAULT_CONNECTOR_CLASSES` 保留当前控制台已有的五个 connector class 值。
- `availableConnectorOptions(...)` 使用 `Class.forName(connectorClass, false, classLoader)` 按 classpath 过滤。
- `MagicDebeziumControllerTest.shouldReturnLoadableConnectorOptions` 验证可加载类会返回。
- `MagicDebeziumControllerTest.shouldFilterUnavailableDefaultConnectorOptions` 验证当前 autoconfigure 测试 classpath 中不可加载的默认 connector 会被过滤。
- `MagicDebeziumControllerTest.shouldUseConnectorClassAsTextAndValue` 验证返回项的 `text` 和 `value` 一致。

场景覆盖：

- 默认 connector 可用时返回：由 helper 行为和运行时 classpath 过滤覆盖。
- connector class 不可用时省略：由测试和实现覆盖。

### Preserve existing custom connector values

已覆盖。

- `mergeCurrentConnectorOption()` 在当前 `props.info.connectorClass` 不在选项列表中时追加本地选项。
- `watch(() => props.info && props.info.connectorClass, ...)` 覆盖外层编辑对象异步加载后的补齐。
- 服务层对 HTTP/网络 `.error` 和 Magic 业务 `.exception` 都会调用 failure 回调。
- 请求失败时组件只调用 `mergeCurrentConnectorOption()`，不会清空 `connectorOptions` 或 `info.connectorClass`。

场景覆盖：

- 编辑带自定义 connector 的数据源：由当前值补齐逻辑覆盖。
- 动态选项请求失败：由服务失败回调和组件失败处理覆盖。

## 设计一致性

- 实现遵循“后端候选列表 + classpath 过滤 + 前端当前值补齐”的设计决策。
- 未新增 Debezium connector 依赖。
- 未修改数据源 JSON 结构、测试接口、Debezium 引擎启动、Kafka 配置或故障恢复行为。
- 前端 service 注入方式符合宿主控制台运行时：`magic-main.vue` 提供 `services` map，Debezium 表单读取 `services['debezium-datasource']`。

## 问题

### CRITICAL

无。

### WARNING

- Debezium console 插件当前没有前端组件测试脚手架，本轮也未启动完整 Magic-API 宿主控制台做浏览器级手动验证。缓解措施：Vite 构建已通过，相关 Vue/service 代码路径已按 OpenSpec 场景检查，并完成 standard 代码审查。

### SUGGESTION

- 后续若控制台补齐组件或 E2E 测试体系，可增加 connector options 加载、当前值补齐、请求失败兜底三个场景的自动化测试。

## 最终评估

未发现 Critical 问题。已记录一个前端浏览器级验证限制。实现满足本次 OpenSpec change，可进入归档前确认。
