## Context

Debezium 插件控制台的 `magic-debezium-datasource-info.vue` 直接维护 `connectorOptions` 常量，并把它传给 `magic-select`。后端 `MagicDebeziumController` 当前主要提供数据源测试能力，实际 Debezium 引擎创建逻辑位于 `MagicDynamicDebeziumDataSource`，数据源动态注册由 `DebeziumDataSourceMagicDynamicRegistry` 负责。

本次需求来自数据源编辑体验：connector class 的可选项应由运行时或插件后端统一提供，而不是散落在前端构建产物中。项目约束包括 JDK 1.8、Spring Boot 2.7.18、现有 Magic-API 插件控制台请求封装，以及不引入新的 Debezium connector 依赖。

## Goals / Non-Goals

**Goals:**

- 前端 Debezium 数据源表单从动态接口加载 `connectorClass` 下拉选项。
- 后端集中定义并返回当前支持的 Debezium connector class 选项。
- 已保存但不在动态列表内的 connector class 在编辑时仍可展示和保留。
- 保持现有数据源 JSON 字段、测试接口和引擎启动行为兼容。

**Non-Goals:**

- 不新增 MySQL/PostgreSQL/SQL Server/Oracle/MongoDB 之外的 Debezium connector 依赖。
- 不实现运行时安装 connector jar 或插件热加载。
- 不修改 Debezium 引擎配置生成、Kafka topic 规则、故障恢复逻辑。
- 不调整 Magic-API 通用资源管理交互。

## Decisions

1. 后端提供 Debezium connector options 接口。

   方案：在 Debezium 插件后端新增只读接口，例如 `/debezium/datasource/connector-options`，返回 `[{ text, value }]` 或等价 DTO。前端服务封装新增方法调用该接口。

   理由：connector 支持范围属于运行时能力，后端比前端更接近 classpath、配置和版本事实。相比继续在 Vue 组件内维护常量，这能减少前后端漂移；相比让前端读取外部 JSON，后端接口更容易跟 Spring 配置、条件装配和权限模型集成。

2. 默认连接器列表由后端集中维护，并可按实际 classpath 做可用性过滤。

   方案：后端维护当前默认 connector class 候选集，并在返回前检查 class 是否可加载；不可加载的候选项不返回。若后续需要用户配置扩展，可在同一服务层扩展配置属性，不影响前端协议。

   理由：完全扫描 classpath 查找 connector 实现成本高、结果不稳定，也可能暴露非预期类；显式候选集加 classpath 过滤更可控。相比纯配置列表，该方案保留开箱即用体验。

3. 前端保留已有值兼容。

   方案：组件加载动态选项后，如果 `info.connectorClass` 存在且不在返回列表中，则临时追加一个 `{ text: value, value }` 选项供当前表单展示。保存时仍保存原字段值。

   理由：老配置或用户手工填过的自定义 connector 不应因为下拉列表变化而在编辑时不可见或被清空。

4. 接口失败时不阻断表单编辑。

   方案：前端请求失败时至少保留当前 `info.connectorClass` 作为可选项，并允许用户继续编辑其他字段；错误不应影响已有数据源查看。

   理由：动态选项是编辑辅助能力，不应成为读取/保存已有配置的单点故障。

## Risks / Trade-offs

- 动态接口返回空列表 → 表单下拉没有默认候选项。缓解：后端默认候选集覆盖现有五类 connector，并在测试中验证至少返回 classpath 中可用项。
- classpath 过滤导致某些历史硬编码选项不再展示 → 已有值兼容逻辑会把当前值补回，但新建数据源只展示实际可用项。
- 前端异步加载存在短暂空状态 → 使用响应式 `connectorOptions` 初始化为空，并在加载后更新；必要时保留当前值避免视觉跳动影响保存。
- 后端 DTO 与 Magic Select 结构耦合 → 可先返回通用 `text/value` 结构，保持与现有前端组件一致；若后续有更多元数据，再增量扩展字段。

## Migration Plan

1. 后端新增 connector options 查询能力，默认候选集与当前前端硬编码列表保持一致。
2. 前端服务新增获取 connector options 方法。
3. Vue 组件删除本地硬编码 `connectorOptions` 常量，改为加载服务数据并补齐当前值。
4. 验证已有 Debezium 数据源 JSON 不需要迁移；失败回滚时恢复前端常量即可。

## Open Questions

无阻塞问题。后续实现时可以根据现有 controller 风格确定精确返回包装类型和 URL 命名。
