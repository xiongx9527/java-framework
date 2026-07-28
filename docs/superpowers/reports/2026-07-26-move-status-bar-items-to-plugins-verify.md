# move-status-bar-items-to-plugins 验证报告

## 摘要

| 维度 | 状态 |
| --- | --- |
| 完整性 | PASS：OpenSpec 8/8 任务完成，`openspec validate --strict` 通过 |
| 正确性 | PASS：状态栏、toolbar、Plugin attributes、IIFE 资源装配与 WS/MQTT 运行时基址均有测试或复审证据 |
| 一致性 | PASS：实现符合 design.md 与技术设计文档的插件自注册、宿主解耦、稳定排序和 Boot/Cloud 分层决策 |

最终结论：PASS。未发现 CRITICAL 或 IMPORTANT 问题，可以进入 archive 前确认。

## 验证范围

- Change：`move-status-bar-items-to-plugins`
- 分支：`dev`
- Verify 模式：`full`
- 最新实现提交：
  - `a6724bd4 fix: preserve plugin resources and backend urls`
  - `b45a11fa chore: advance plugin status change to verify`
- 说明：工作区仍保留 3 个未提交的 LangChain4j `qdrant-datasource -> vector-datasource` 改动。该 diff 未纳入本报告的通过判断，未被提交，也未被本次验证修改。

## 执行命令

| 命令 | 结果 |
| --- | --- |
| `comet state check move-status-bar-items-to-plugins verify` | PASS |
| `comet state scale move-status-bar-items-to-plugins` | full |
| `openspec status --change move-status-bar-items-to-plugins --json` | PASS，8/8 tasks done |
| `openspec instructions apply --change move-status-bar-items-to-plugins --json` | PASS，proposal/design/spec/tasks 均存在 |
| `openspec validate move-status-bar-items-to-plugins --strict` | PASS |
| `node --test fw-plus-boot/.../fw-plus-magicapi-spring-boot-starter/src/console/test/plugin-status-bar.contract.test.mjs` | PASS，3/3 |
| `node --test fw-plus-boot/.../fw-plus-magicapi-plugin-ws/src/console/test/open-target.test.mjs fw-plus-boot/.../fw-plus-magicapi-plugin-mqtt/src/console/test/open-target.test.mjs` | PASS，6/6 |
| `mvn -o -nsu -DfailIfNoTests=false -pl ...boot-autoconfigure,...jdbc-starter,...admin-starter,...druid,...admin,...swagger -Dtest=PluginFrontendMetadataTest,MagicApiDruidFrontendClasspathTest,MagicApiAdminFrontendClasspathTest,MagicApiSwaggerFrontendClasspathTest test` | PASS，Boot autoconfigure 3/3，Druid/Admin/Swagger classpath 契约各 1/1 |
| `mvn -o -nsu -pl fw-plus-cloud-project/fw-plus-cloud-autoconfigure -Dtest=MagicAPIEurekaConfigurationTest test` | PASS，1/1；未访问 `172.17.1.6:28081` |
| `git diff --cached --check`（提交前） | PASS |
| scoped `git diff --check`（最终修复相关文件） | PASS |

## 需求核对

### 可选插件拥有自己的状态栏入口

PASS。受影响插件通过 IIFE 返回 `statusBarItems`，宿主只通用收集和渲染。Task 8 已记录 16 个状态栏入口浏览器检查，最终修复后 WS/MQTT 与 Druid/Admin/Swagger/Eureka 关键回归点均补充测试。

### 宿主状态栏与具体插件解耦

PASS。Task 7 已删除 `magic-status-bar.vue` 中插件专属 `hasXxxPlugin`、`checkPlugin`、专属点击函数和文案轮询。最终复审未发现宿主重新引入插件名分支。

### 状态栏兼容原有行为

PASS。共享状态栏契约测试 3/3 通过。WS/MQTT 新增 IIFE 行为测试覆盖同源生产路径、绝对后端基址和前后端分离开发模式，确认点击目标使用运行时 `constants.BASE_URL`。

### 插件构建产物携带状态栏声明

PASS。Task 8 已完成 23/23 console 构建、67/67 前端测试和目标 JAR IIFE 检查。最终修复补充 Druid/Admin/Swagger/Eureka 资源存在和缺失元数据契约，避免后端在 classpath 缺少 IIFE 时宣告不可加载的 `globalName/javascriptFilename`。

### 插件运行时属性传入前端工厂

PASS。`Plugin.attributes` 已用于 Admin/Eureka 自定义路径，插件加载器向 IIFE 工厂传入 `pluginData`。相关 Java 和前端契约已覆盖。

### 静态插件 Toolbar 支持稳定排序

PASS。Task 6/8 已通过 AST 契约和前端行为验证，静态插件 toolbar 独立排序，内置和动态 toolbar 不参与排序。

## 复审结果

- Final review 原有 I1/I2 两个 Important 均已修复。
- I1 scoped re-review：PASS。离线 Cloud autoconfigure Eureka 测试 1/1 通过，无剩余问题。
- I2 scoped re-review：PASS。共享状态栏契约测试 3/3 通过，无剩余问题。

## 风险与限制

- 未执行推送。
- 未执行全量根级 reactor。原因：仓库多顶层 Maven 根且用户要求不走 `172.17.1.6:28081`；本轮 Maven 验证均使用 `-o -nsu` 离线执行。
- 当前工作区有 3 个未提交 LangChain4j 改动，保留给后续单独处理。

## 结论

验证通过。该 change 满足 proposal、design、delta spec 和 tasks 的要求，已准备进入 archive 前最终确认。
