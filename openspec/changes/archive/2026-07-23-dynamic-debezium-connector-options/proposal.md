## Why

Debezium 数据源编辑表单目前在前端组件中硬编码 `connectorClass` 下拉选项，支持范围与运行时实际可用的 Debezium connector 依赖容易漂移。需要把连接器选项改为动态来源，让控制台随插件后端能力或配置变化而更新。

## What Changes

- 新增 Debezium connector 选项的动态获取能力，由插件后端暴露当前可选的 connector class 列表。
- 修改 Debezium 数据源编辑组件，使 `connectorClass` 下拉项从动态接口加载，而不是维护前端硬编码数组。
- 保留现有 MySQL、PostgreSQL、SQL Server、Oracle、MongoDB connector class 作为默认可用范围，但默认范围由后端集中维护。
- 当已有数据源保存了当前列表之外的 `connectorClass` 时，表单仍应能展示并保留该值，避免编辑保存时丢失配置。
- 不新增 Debezium connector 依赖，不改变 Debezium 引擎创建、运行、故障恢复或数据源持久化格式。

## Capabilities

### New Capabilities
- `debezium-connector-options`: Debezium 数据源控制台能够从后端动态加载连接器 class 选项，并兼容已有自定义 connector 值。

### Modified Capabilities

无。

## Impact

- 影响前端组件：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/components/magic-debezium-datasource-info.vue`
- 影响前端服务封装：`fw-plus-boot/.../fw-plus-magicapi-plugin-debezium/src/console/src/service/magic-debezium-datasource.js`
- 影响后端控制器：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/web/MagicDebeziumController.java`
- 可能新增后端常量、配置属性或简单 DTO，用于集中定义 connector class 选项。
- 验证需覆盖前端构建/静态检查，以及后端接口在 JDK 1.8 与 Spring Boot 2.7.18 下编译通过。
