# Brainstorm Summary

- Change: dynamic-debezium-connector-options
- Date: 2026-07-23

## 确认的技术方案

采用后端作为 Debezium connector 选项事实源。后端在 Debezium 插件控制器中新增只读接口 `/debezium/datasource/connector-options`，返回 `text/value` 结构的连接器选项。默认候选列表由后端集中维护，内容覆盖当前前端硬编码的 MySQL、PostgreSQL、SQL Server、Oracle、MongoDB connector class；返回前按运行时 classpath 过滤不可加载的 connector class。

前端 Debezium 控制台服务新增 `loadConnectorOptions` 方法调用该接口。`magic-debezium-datasource-info.vue` 删除本地硬编码 `connectorOptions` 常量，改为在组件挂载和当前 `info.connectorClass` 变化时维护响应式选项。如果当前数据源已有 connector class 不在后端返回列表中，前端将当前值临时补充为本地选项，保证编辑旧配置或自定义配置时不丢值。

## 关键取舍与风险

- 后端事实源能减少前后端漂移，并反映当前服务端依赖状态；代价是新增一个小型只读接口和选项结构。
- classpath 过滤会让未启用依赖的 connector 不出现在新建表单默认选项中；这符合运行时事实。已有数据源的当前值由前端补回，不影响编辑保存。
- 完全扫描 classpath 查找 connector 实现不采用，因为结果不稳定、成本更高，也可能暴露非预期类；显式候选列表加可加载性过滤更可控。
- 接口失败不能阻断表单编辑；前端必须保留当前 `connectorClass`，让用户仍可查看和保存其他字段。

## 测试策略

- 后端验证 connector options 接口返回 classpath 可加载的默认候选项，并过滤不可加载候选项。
- 前端验证连接器选项能异步加载，当前自定义值能补回，接口失败时当前值不被清空。
- 构建验证使用 `fw-plus-boot/mvnw` 编译受影响 Maven 模块，并在 Debezium console 目录执行 `npm run build`。
- OpenSpec 使用 `openspec validate dynamic-debezium-connector-options` 确认 delta spec 有效。

## Spec Patch

无。
