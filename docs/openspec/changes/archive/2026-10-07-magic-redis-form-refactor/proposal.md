# Proposal

## Why

redis datasource 插件的控制台表单是纯硬编码实现，与参照实现 `fw-plus-ai-magicapi-plugin-langchain4j-chat-model` 的 metadata 驱动结构脱节：没有字段元数据、没有必填标记与前端校验、i18n 只有标签级文案、service 中残留失效代码（`com.jugui` 包名全仓不存在、`findResources('nebula-datasource')` 笔误）。同时存在真实缺陷：表单绑定 `info.propertiesText` 而后端实体字段为 `properties(Map)`，全仓无任何转换点，导致连接属性保存即丢失、编辑回显为空。

## What Changes

- 表单改为 metadata 驱动：新增 `src/console/src/metadata/redis-datasource.js` 字段描述符表（`{name, type, required}` + 敏感字段 + select 枚举），组件改为 `v-for` 渲染，与参照实现结构对齐。
- 必填字段显示 `*` 标记，并新增前端拦截提交：控制台 `magic-data-resource.vue` 的 `doSave()` 增加可选的 `service.validate` 通用钩子（未定义该方法的插件行为完全不变），redis 插件 service 实现 `validate`，必填为空时弹窗提示并阻止请求发出。
- 修复 `propertiesText`/`properties` 映射：表单改为对 `info.properties` 的双向 JSON 转换（参照 `magic-langchain4j-aj-mcp-client-info.vue` 的现成模板），同时修复「保存丢失」与「编辑回显为空」。
- 清理 service 失效代码：移除失效的 `JavaClass.setExtensionAttribute`（`com.jugui` 包名）与 `findResources('nebula-datasource')` 笔误，service 收敛为参照同款的 `injectResources`/`doTest`（+ 新增 `validate`）。
- i18n 重构为 `common`/`hint`/`field`/`extra` 细粒度分层，中英文同构，与参照实现对齐。
- 新增 `src/console/test/` 交叉校验测试（`node --test`），校验前端 metadata 字段与后端 `RedisDataSourceInfo`/`BaseService.validateField` 口径一致，`package.json` 增加 `test` script。
- 新增 `README.md` 说明三元组注册、产物名与测试方式（与参照模块对齐）。

## Capabilities

### New Capabilities

- `magicapi-redis-plugin`: redis datasource 插件控制台动态表单的行为契约——metadata 驱动的字段渲染、必填标记与保存前校验、properties JSON 双向绑定、i18n 文案结构、前端 metadata 与后端口径的交叉校验。

### Modified Capabilities

（无——现有 specs 中不存在 redis 插件相关能力，也不修改已有能力的验收场景。）

## Impact

- **前端（插件）**：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-redis/src/console/` 下的 `components/`、`service/`、`i18n/`、新增 `metadata/`、`test/`、`README.md`。
- **前端（共享控制台）**：`fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/main/magic-data-resource.vue` 增加一个可选 `service.validate` 钩子；控制台产物需重新构建。对其它 20+ 插件为向后兼容的空操作（它们均未定义 `validate`）。
- **Java 侧零改动**：`RedisDataSourceInfo`、`RedisDataSourceInfoMagicResourceStorage`、`MagicRedisController`、`MagicAPIRedisConfiguration` 的 `Plugin("redis","MagicRedis","magic-redis.1.0.0.iife.js")` 三元组、Maven 编排均保持不变。
- **后端兜底不变**：`BaseService.validateField` 与 storage `validate()` 仍是最终权威。

## 非目标

- 其余 14 个同类插件（kafka/mongo/es 等）的 `propertiesText` 同源问题——本次只修 redis。
- 控制台既有其它缺陷（如 `dialogWidth: '1100px'` 扩展点未被消费）。
- 产物文件名、IIFE 全局名、Plugin 三元组、运行时（`MagicDynamicRedisDataSource`/注册中心）逻辑。
- 数据库 schema、公共 API、跨模块架构调整。
