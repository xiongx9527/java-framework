# Design

## Context

参照实现 `fw-plus-ai-magicapi-plugin-langchain4j-chat-model` 的控制台采用 metadata 驱动表单：`metadata/providers.js` 声明字段描述符 → 组件 `v-for` 渲染 → i18n 按 `common/hint/field` 分层 → `node --test` 交叉校验元数据与后端工厂口径。redis 插件当前为纯硬编码表单，且存在两个真实问题：service 中引用全仓不存在的 `com.jugui` 类与 `nebula-datasource` 笔误；`propertiesText` 绑定与后端 `properties(Map)` 之间没有任何转换点（保存必丢、回显必空）。

控制台保存入口唯一：`magic-data-resource.vue` 的 `doSave()` 直接 `POST /resource/file/{type}/save`，不经过插件 service，现有机制无法阻止保存；脚本编辑器侧存在「保存前钩子抛错即阻断」的既有先例可参照。运行时与后端校验（`BaseService.validateField`、storage `validate()`）保持不动，仍是最终权威。

## Goals / Non-Goals

**Goals:**

- redis 插件控制台表单重构为与参照实现同构的 metadata 驱动结构。
- 必填星号 + 保存前前端拦截（可选钩子），properties JSON 双向绑定不丢数据。
- 前端元数据与后端口径由自动化测试守护。

**Non-Goals:**

- 不改 Java 侧字段模型、存储、Controller、Plugin 三元组与运行时注册逻辑。
- 不修其余 14 个插件的同源 `propertiesText` 问题。
- 不修控制台其它既有缺陷（如 `dialogWidth` 扩展点未被消费）。
- 不做历史已丢失 properties 数据的恢复。

## Decisions

### D1：metadata 驱动渲染，仿参照而非抽公共渲染器

新建 `src/console/src/metadata/redis-datasource.js`（字段描述符 + 敏感字段名单 + select 枚举），组件改为 `v-for` 渲染。
**备选**：抽公共「元数据渲染器」组件给全部插件用——被否：参照实现与控制台均无此机制，属跨插件架构调整，超出本次范围。

### D2：保存前拦截采用「控制台可选 `service.validate` 钩子」

在 `magic-data-resource.vue` 的 `doSave()` 开头增加：若 `service.validate` 存在则调用，抛错 → 提示错误文案 → `return`（镜像 `magic-script-editor.vue` 保存前钩子的阻断方式）；redis service 实现 `validate(info)` 检查必填与 JSON 合法性。
**备选**（均被否）：axios 请求拦截器——全局副作用、报错文案不可控；组件内禁用保存按钮——按钮在父组件，子组件拿不到；仅靠后端校验——用户已明确要求前端拦截。
**兼容性**：20+ 插件均未定义 `validate`，未定义即跳过，行为零变化。

### D3：properties 采用前端 computed 双向转换

表单 JSON 编辑框改绑本地 computed：`get` = `JSON.stringify(info.properties || {}, null, 2)`，`set` = 解析后写回 `info.properties`；解析失败在 `validate` 中拦截保存。模板直接参照 `magic-langchain4j-aj-mcp-client-info.vue` 的现成实现。
**备选**：后端 `@JsonSetter("propertiesText")` 兜底——被否：本次保持 Java 零改动；且后端兜底无法解决编辑回显。

### D4：service 收敛为参照同款能力集

移除失效的 `JavaClass.setExtensionAttribute`（`com.jugui` 包名不存在，且 Java 类补全由既有 `injectResources` 负责），修正 `findResources('nebula-datasource')` 笔误并入移除范围；service 最终只含 `injectResources`、`doTest`、`validate`、`dialogWidth`。

### D5：交叉校验测试仿参照的源码解析方案

新增 `src/console/test/`，用 `node --test` + 正则解析 `RedisDataSourceInfo.java` 字段与 `BaseService.validateField` 必填集合，与 metadata 字段表比对；`package.json` 增加 `test` script。
**备选**：JVM 反射测试——需要起 Java 环境，与前端构建链路不匹配，被否。

### D6：控制台产物随 Maven 构建重建

`magic-data-resource.vue` 属共享控制台，改后需重建 console 产物（`npm run build`）；插件侧 vite IIFE 配置、产物名 `magic-redis.1.0.0.iife.js`、全局名 `MagicRedis` 均不变。

## Risks / Trade-offs

- [共享控制台改动影响其它插件] → 钩子为可选、未定义即跳过；验证时逐一打开 2+ 个其它 datasource 弹窗确认保存流程无变化。
- [控制台 bundle 重建产生大 diff] → 只重建并提交本 change 必需的产物文件，构建后 `git status` 核对归属。
- [移除 `setExtensionAttribute` 意外影响某功能] → 该调用引用的类全仓不存在，原调用链已失效；移除前后打开编辑弹窗对比 JavaClass 补全行为。
- [正则解析 Java 源的测试较脆弱] → 沿用参照实现的成熟写法，测试失败信息指明漂移字段，便于同步。
- [当前 dev 目录有其它未提交改动] → Build 前按 dirty-worktree 核对归属，只提交本 change 文件。
- [历史 properties 数据已丢失] → 属非目标；运行时实际生效值以配置中心 `magic.api.redis.{key}.properties` 为准。

## Migration Plan

无数据与配置迁移。回滚方式：revert 本 change 的提交即可恢复原表单行为；控制台与插件产物随源码回滚重新构建。

## Open Questions

（无——所有影响方案与任务拆分的决定均已确认。）

## 流程确认记录

- 2026-10-07 升级判定：改动文件数超过 tweak 提示阈值（10 个），按规则提交用户选择；用户确认**继续 tweak 轻量流程**。确认原因：文件数多为 metadata/test/README 等 Open 已确认范围内的配套展开，未出现新增能力、公共 API、schema 变更或跨模块架构协调等实质风险信号，技术方案已在本文档明确。
