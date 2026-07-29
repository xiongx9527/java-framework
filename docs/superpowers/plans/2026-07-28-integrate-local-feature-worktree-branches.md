# Local Feature And Worktree Branch Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将八条本地候选分支中仍有效的代码与测试精选整合到最新 `dev`，统一重叠实现并排除过期流程产物。

**Architecture:** 从已包含设计与计划的最新 `dev` 创建隔离整合分支，按 MCP、POI、Template、MQTT、EasyTrans、Nebula JDBC 六个模块组依次移植。前五组优先复用候选提交中的代码/测试补丁；Nebula 因目录和包名已重构，按旧分支测试契约在当前 `fw-plus-graph` 架构中重放行为。每组独立测试和提交，最终完成 Core -> Boot 分层验证后以 `--ff-only` 推进 `dev`。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、JUnit、Mockito、Maven 3.6.3、Git worktree。

## Global Constraints

- Core 生产代码保持 Java 1.8，不使用 Java 9+ API。
- Magic-API 保持项目维护的 `org.ssssssss:2.2.2`，不调整依赖版本或内部 Nexus 配置。
- 使用系统 `mvn`；当前 `dev` 已移除 Maven Wrapper 文件。
- 只整合目标模块代码和测试，不引入候选分支的 `.superpowers/`、`.comet/`、历史 OpenSpec change、旧计划或任务报告。
- 不直接整体 merge 八条候选分支，不推送远端，不删除本地分支或 worktree。
- 主工作区 `dev` 在最终快进前必须干净；若 `dev` 前进，先将整合分支重放到最新 `dev` 并重新验证。

---

### Task 1: 创建隔离工作区并冻结基线

**Files:**
- Reference: `docs/superpowers/specs/2026-07-28-integrate-local-feature-worktree-branches-design.md`
- Reference: `docs/superpowers/plans/2026-07-28-integrate-local-feature-worktree-branches.md`

**Interfaces:**
- Consumes: 当前干净的 `dev`、本计划列出的八条候选分支。
- Produces: `.worktrees/integrate-local-feature-worktree-branches` 和 `codex/integrate-local-feature-worktree-branches`。

- [ ] **Step 1: 验证主工作区和 worktree 目录安全性**

Run:

```bash
git status --short --branch
git rev-parse dev
git check-ignore -q .worktrees
git worktree list --porcelain
```

Expected: 主工作区无未提交文件，`.worktrees` 已被忽略，记录 `dev` 基准提交为 `INTEGRATION_BASE`。

- [ ] **Step 2: 创建隔离分支和 worktree**

Run:

```bash
git worktree add .worktrees/integrate-local-feature-worktree-branches \
  -b codex/integrate-local-feature-worktree-branches dev
```

Expected: 新 worktree 位于计划路径，分支 HEAD 等于 `INTEGRATION_BASE`。

- [ ] **Step 3: 重新审计候选补丁**

Run in the integration worktree:

```bash
for branch in \
  feature/20260708/notify-mcp-config-changes \
  feature/20260713/stabilize-fw-plus-poi \
  worktree-feature-20260714-optimize-fw-plus-template \
  worktree-mqtt-plugin-optimization \
  worktree-optimize-easytrans \
  worktree-optimize-fw-plus-dify \
  worktree-stabilize-fw-plus-nebula-jdbc \
  worktree-stabilize-nebula-jdbc
do
  git cherry dev "$branch"
done
```

Expected: 输出作为最新基准证据；后续只处理 `+` 补丁及其表达但尚未由当前测试覆盖的行为。

- [ ] **Step 4: 运行受影响模块基线测试**

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml test
mvn -f fw-plus-core/fw-plus-project/fw-plus-template/pom.xml test
mvn -f fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml test
mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/pom.xml test
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -DskipTests compile
```

Expected: 基线通过；若失败，记录原始错误并在修改前判断是否属于现有环境问题。

---

### Task 2: 整合 MCP 配置变更通知

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistry.java`
- Verify/Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifier.java`
- Verify/Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/config/service/ConfigMagicDynamicRegistryTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/notification/McpConfigurationChangeNotifierTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java`

**Interfaces:**
- Consumes: Magic-API config registry 成功注册结果和 MCP 配置前缀。
- Produces: 只在注册成功后发布的配置变更事件；只通知 MCP 相关配置。

- [ ] **Step 1: 对比当前实现与候选功能提交**

Run:

```bash
git show fd993d0d -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src
git show 7951f041 7858d447 4dc214d8 -- fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src
git diff dev..feature/20260708/notify-mcp-config-changes -- \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src
```

Expected: 明确当前 `dev` 已具备的通知器/配置装配，以及仍缺少的“成功后发布”和前缀过滤断言；不采用只修改报告的 `b9ebd416`。

- [ ] **Step 2: 先补齐缺失的回归断言并验证失败**

Apply only missing assertions from `7951f041`, `7858d447`, and the test portion of `4dc214d8` with `apply_patch`.

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=ConfigMagicDynamicRegistryTest,McpConfigurationChangeNotifierTest,MagicAPIMcpConfigurationTest test
```

Expected: 若当前实现缺少目标行为，新增断言失败；若全部通过，将对应实现记为语义等价并不改生产代码。

- [ ] **Step 3: 移植最小生产代码**

Apply the production hunks from `fd993d0d` and `4dc214d8` that are still missing. Preserve current `dev` constructor/API shape. The registry must call the notification path only after `super.register(...)` reports success, and MCP filtering must reject unrelated config keys.

- [ ] **Step 4: 运行 MCP 定向测试并提交**

Run the Task 2 test command again. Expected: all selected tests pass.

Commit:

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java
git commit -m "feat(magicapi): 整合 MCP 配置变更通知"
```

---

### Task 3: 统一 POI 边界处理

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/ExcelExportUtil.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelBatchExportService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelExportService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransService.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/main/java/cn/afterturn/easypoi/excel/imports/FwPlusExcelImportService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/ExcelExportUtilFwPlusBoundaryTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/src/test/java/cn/afterturn/easypoi/excel/export/FwPlusExcelTransServiceTest.java`

**Interfaces:**
- Consumes: EasyPoi export/import params、批量数据、翻译集合和模板数据。
- Produces: 对 null/empty 输入稳定的导入导出行为，模板导出保留翻译结果。

- [ ] **Step 1: 将两个 POI 分支的测试契约合并为一套**

Inspect `6290007e`, `9e7bff4f`, `faa63b0e`, `7f17f61d`, `116de7b2`, `a748b3a6`, `fe9fecdb`, `dc784f12`, and `93c2ceff`. Use `apply_patch` to add only non-duplicate JUnit Jupiter assertions to the two test classes and the minimal test dependency in `pom.xml`.

- [ ] **Step 2: 运行测试确认当前缺口**

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml \
  -Dtest=ExcelExportUtilFwPlusBoundaryTest,FwPlusExcelTransServiceTest test
```

Expected: 未由当前 `dev` 覆盖的边界断言失败；已经覆盖的断言通过。

- [ ] **Step 3: 整合生产代码而不顺序覆盖重叠文件**

Use the final behavior from `f04442c7`, `005053ab`, `03ecb67d`, `24e51249`, `372ec39e`, and `0e921ca8`. For `FwPlusExcelTransService`, compare both fix commits and implement one combined null-safe path; do not cherry-pick one branch over the other.

- [ ] **Step 4: 运行 POI 模块测试并提交**

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi/pom.xml test
```

Expected: module tests pass.

Commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-poi/fw-plus-poi-easypoi
git commit -m "fix(poi): 整合导入导出边界保护"
```

---

### Task 4: 整合 Template Parser 能力边界

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/TemplateParser.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-template/src/main/java/com/xiongx9527/fw/plus/template/excel/ExcelParser.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/TemplateParserTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-template/src/test/java/com/xiongx9527/fw/plus/template/ParserConversionBoundaryTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModule.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/template/TemplateParserConfigurationTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template/TemplateModuleTest.java`

**Interfaces:**
- Consumes: parser 输入流/URL/数据与可选 Word、Excel、HTML parser beans。
- Produces: 先校验输入再判断解析能力；Magic-API Word URL 走正确 parser；缺失可选能力时自动配置稳定。

- [ ] **Step 1: 只移植候选测试并执行红灯验证**

Apply only the POM/test hunks from `f7a28878`, `acc3ced0`, `cf9e02f0`, `3eb2fc6c`, and `e43fe9d7`; exclude production hunks and all workflow documents.

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-template/pom.xml \
  -Dtest=TemplateParserTest,ParserConversionBoundaryTest test
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=TemplateParserConfigurationTest,TemplateModuleTest test
```

Expected: assertions missing from current behavior fail before final production fix.

- [ ] **Step 2: 应用候选生产代码及最终输入校验顺序**

Apply the production hunks from `f7a28878`, `acc3ced0`, `cf9e02f0`, `3eb2fc6c`, and `e43fe9d7`, preserving Java 8 APIs and current exception types. Input validity checks must happen before dereferencing or querying parser capabilities.

- [ ] **Step 3: 运行 Core 与 Boot 定向测试并提交**

Run the Task 4 test commands again. Expected: all selected tests pass.

Commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-template \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/template \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/template \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/modules/template
git commit -m "fix(template): 整合解析能力边界"
```

---

### Task 5: 整合 MQTT 插件生命周期

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/module/MqttModule.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupport.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/consumer/MqttClientConsumer.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/model/MagicDynamicMqttClientDataSource.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModule.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/producer/MqttClientProducer.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistry.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientSubscribeMagicDynamicRegistry.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/starter/MagicAPIMqttClientConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt/starter/MagicAPIMqttConfigurationTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/common/MqttClientConfigSupportTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/consumer/MqttClientConsumerTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/module/MqttClientModuleTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient/service/MqttClientDataSourceMagicDynamicRegistryTest.java`

**Interfaces:**
- Consumes: MQTT datasource/subscription 配置与 Magic-API 资源注册事件。
- Produces: 共享配置解析、持久化成功后注册、刷新时保留覆盖配置、禁用/删除时关闭消费者。

- [ ] **Step 1: 无提交地应用 11 个代码提交并排除流程文件**

Run:

```bash
git cherry-pick -n 2eaba653 74e48594 9605bfab b78b67b7 68326c46 \
  b5223b7d 695cff20 2d6a4ff4 fb5769fd 2a3f107f ad55acea
git restore --staged --worktree -- .superpowers .comet openspec docs/superpowers
```

Resolve conflicts by preserving current `dev` APIs and the final branch behavior. Do not use `--ours` or `--theirs` over whole files.

- [ ] **Step 2: 运行生命周期定向测试**

Run:

```bash
mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MagicAPIMqttConfigurationTest,MqttClientConfigSupportTest,MqttClientModuleTest,MqttClientDataSourceMagicDynamicRegistryTest,MqttClientConsumerTest,MagicAPIMqttClientConfigurationTest test
```

Expected: datasource 删除/刷新、disabled subscription、持久化失败和配置覆盖场景通过。

- [ ] **Step 3: 检查注册顺序和资源关闭路径**

Verify in the diff that `MqttClientSubscribeMagicDynamicRegistry.register(...)` persists before registering the live subscription, datasource removal closes its consumer, refresh preserves subscription overrides, and disabled subscriptions are unsubscribed.

- [ ] **Step 4: 提交 MQTT 整合**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqtt \
        fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mqttclient
git commit -m "fix(mqtt): 整合客户端配置与生命周期"
```

---

### Task 6: 整合 EasyTrans 空值与递归保护

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/main/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtils.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/src/test/java/com/xiongx9527/fw/plus/easytrans/service/util/MateEasyTransUtilsTest.java`

**Interfaces:**
- Consumes: 单对象/批量翻译输入、包含共享引用或代理对象的对象图。
- Produces: null 元素安全、基于对象身份的递归去重、代理访问失败时安全回退。

- [ ] **Step 1: 应用测试与实现提交，排除报告文件**

Run:

```bash
git cherry-pick -n b9d1f756 5626cbb8 56952fa5 ac7764f7 8fcce6f7
git restore --staged --worktree -- .superpowers .comet openspec docs/superpowers
```

Resolve against current `MateEasyTransUtils` without changing public signatures. `34bb1e3e` only修改报告，不应用。

- [ ] **Step 2: 运行 EasyTrans 测试**

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service/pom.xml \
  -Dtest=MateEasyTransUtilsTest test
```

Expected: null batch element、共享引用、递归对象图和代理回退场景通过。

- [ ] **Step 3: 提交 EasyTrans 整合**

```bash
git add fw-plus-core/fw-plus-project/fw-plus-easytrans/fw-plus-easytrans-service
git commit -m "fix(easytrans): 整合空值与递归保护"
```

---

### Task 7: 在当前 Graph 架构中重放 Nebula JDBC 契约

**Files:**
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaConnection.java`
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/NebulaDriver.java`
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/NebulaJdbcUrlParser.java`
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/config/NebulaConnectionConfig.java`
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/config/NebulaConnectionConfigParser.java`
- Verify/Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycle.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/utils/NebulaJdbcUrlParserTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/config/NebulaConnectionConfigParserTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/lifecycle/NebulaResourceLifecycleTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/client/NebulaClientFacadeTest.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/nebula/jdbc/statement/NebulaUnsupportedJdbcFeatureTest.java`

**Interfaces:**
- Consumes: Nebula JDBC URL、连接属性、pool range、native client 和 close actions。
- Produces: 明确拒绝非法地址/端口/范围、使用统一解析配置、close 幂等且资源释放顺序确定。

- [ ] **Step 1: 从旧分支提取行为矩阵，不直接 cherry-pick 路径**

Inspect `ae10f557`, `3ba8c317`, `f6019f6e`, `27114ffc`, `c2ad8ae2`, `9acafea3`, and `655bca77`. Map old `com.vesoft.nebula.jdbc` and `fw-plus-nebula` paths to current `com.xiongx9527.fw.plus.graph.nebula.jdbc` and `fw-plus-graph` paths.

Required assertions:

```text
invalid or missing host -> SQLException/URISyntaxException with stable message
non-numeric or out-of-range port -> checked failure, no raw NumberFormatException
invalid min/max pool range -> rejected before native client creation
parsed URL/default properties -> one NebulaConnectionConfig source of truth
close called twice -> resources released once, connection remains closed
unsupported JDBC methods -> documented SQLFeatureNotSupportedException behavior
```

- [ ] **Step 2: 先补当前包结构下缺失的测试并运行**

Add the URL validation matrix to `NebulaJdbcUrlParserTest`; add only missing pool/config cases to `NebulaConnectionConfigParserTest`, idempotent close cases to `NebulaResourceLifecycleTest`, native failure cleanup cases to `NebulaClientFacadeTest`, and unsupported-method assertions to `NebulaUnsupportedJdbcFeatureTest`.

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/pom.xml \
  -Dtest=NebulaJdbcUrlParserTest,NebulaConnectionConfigParserTest,NebulaResourceLifecycleTest,NebulaClientFacadeTest,NebulaUnsupportedJdbcFeatureTest test
```

Expected: only genuinely missing old-branch contracts fail; existing current architecture tests remain green.

- [ ] **Step 3: 在当前组件边界中实现缺失行为**

Place URL token validation in `NebulaJdbcUrlParser`/`NebulaConnectionConfigParser`, pool validation in `NebulaConnectionConfig`, native resource ownership in `NebulaResourceLifecycle`, and connection state transitions in `NebulaConnection`. Do not restore deleted old packages or duplicate lifecycle abstractions.

- [ ] **Step 4: 运行 Nebula JDBC 全模块测试并提交**

Run:

```bash
mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/pom.xml test
```

Expected: all module tests pass under Java 8.

Commit:

```bash
git add fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc
git commit -m "fix(nebula-jdbc): 整合解析与生命周期契约"
```

---

### Task 8: 全量审计、分层验证并快进 dev

**Files:**
- Verify only: all files changed by Tasks 2-7.

**Interfaces:**
- Consumes: 六个模块整合提交。
- Produces: 经验证且可由 `dev` 快进接收的整合分支。

- [ ] **Step 1: 审计差异范围和 Java 8 兼容性**

Run:

```bash
git status --short
git diff --check dev...HEAD
git diff --name-only dev...HEAD
git diff dev...HEAD -- .superpowers .comet openspec docs/superpowers
git diff -U0 dev...HEAD -- '*.java' | rg '^\+.*(List\.of|Map\.of|Map\.copyOf|Set\.of|Optional\.orElseThrow\(\))' || true
```

Expected: worktree clean；流程目录无差异；无 Java 9+ API；文件范围仅包含六个模块的生产代码和测试。

- [ ] **Step 2: 安装 Core 受影响模块及依赖**

Run:

```bash
mvn -f fw-plus-core/pom.xml \
  -pl :fw-plus-poi-easypoi,:fw-plus-template,:fw-plus-easytrans-service,:fw-plus-nebula-jdbc \
  -am clean install
```

Expected: Core reactor succeeds with zero test failures.

- [ ] **Step 3: 验证 Boot autoconfigure**

Run:

```bash
mvn -f fw-plus-boot/pom.xml -pl :fw-plus-boot-autoconfigure -am clean test
```

Expected: Boot reactor succeeds with zero test failures.

- [ ] **Step 4: 复核候选行为覆盖和提交序列**

Run:

```bash
git log --oneline --decorate dev..HEAD
git diff --stat dev...HEAD
git status --short --branch
```

Expected: 每个实际产生差异的模块有一个独立提交；语义等价模块可无生产代码提交，但必须有测试证据；工作区干净。

- [ ] **Step 5: 检查 dev 并发变化并快进**

Run from the main workspace:

```bash
git status --short --branch
git merge-base --is-ancestor dev codex/integrate-local-feature-worktree-branches
git merge --ff-only codex/integrate-local-feature-worktree-branches
```

Expected: 若祖先检查成功，`dev` 快进到整合分支；若失败，停止快进，在隔离 worktree 中将整合提交重放到最新 `dev` 并重新执行 Task 8 全部验证。

- [ ] **Step 6: 最终验证**

Run from the main workspace:

```bash
git status --short --branch
git log --oneline --decorate -10
git branch --contains HEAD --format='%(refname:short)'
```

Expected: 当前分支为 `dev`，工作区干净，整合提交均包含在 `dev`；不执行 push 或分支删除。
