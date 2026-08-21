---
change: move-status-bar-items-to-plugins
design-doc: docs/superpowers/specs/2026-07-25-move-status-bar-items-to-plugins-design.md
base-ref: a4d3a4a2c7b5962d0d5324761f353e75a5443df5
archived-with: 2026-07-26-move-status-bar-items-to-plugins
---

# Magic-API 插件状态栏与 Toolbar 排序实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将全部可选插件状态栏入口迁入各自前端插件，并为状态栏和静态插件 toolbar 提供稳定、可兼容的 `order` 排序。

**Architecture:** 前端 IIFE 返回 `statusBarItems` 和 `toolbars`，宿主只执行通用校验、过滤、稳定排序和渲染。后端 `Plugin.attributes` 只传运行时数据，加载器通过 `pluginData` 交给前端工厂，并用专用事件通知插件集合变化。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API 2.2.2、Vue 3、Vite、Node.js `node:test`、JUnit 5、Maven。

## Global Constraints

- Java 代码必须兼容 JDK 8，不使用 Java 9+ API。
- `statusBarItems` 完全由前端插件声明，后端不得声明图标、标题、顺序或点击动作。
- 插件状态栏固定在在线人数之后、宿主内置入口之前。
- 只排序静态插件 toolbar；不改变内置 toolbar、动态 `ADD_FOOTER_TOOLBAR` 或 `adminOnly` 行为。
- 不手工修改压缩 IIFE；所有产物必须从 `src/console` 构建。
- 不改变现有业务接口、管理页面、数据源格式和权限模型。

---

### Task 1: 公共运行时元数据与插件加载事件

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-magicapi/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-magicapi/src/main/java/org/ssssssss/magicapi/core/model/Plugin.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-magicapi/src/test/java/org/ssssssss/magicapi/core/model/PluginTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/constants/message.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/plugin-loader.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/magic-editor.vue`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/test/plugin-loader.contract.test.mjs`

**Interfaces:**
- Produces: `Plugin#getAttributes(): Map<String, Object>`，默认非空。
- Produces: IIFE 工厂参数 `pluginData`，值为 `/plugins` 返回的当前插件对象。
- Produces: `Message.PLUGINS_CHANGED`，插件成功加入 `constants.PLUGINS` 后触发。

- [x] **Step 1: 补充测试依赖并编写失败的 Java 测试**

在 `fw-plus-magicapi` 增加与同层测试模块一致的 `spring-boot-starter-test` 测试依赖。

```java
@Test
void attributesAreMutableAndEmptyByDefault() {
    Plugin plugin = new Plugin("sample");
    assertNotNull(plugin.getAttributes());
    assertTrue(plugin.getAttributes().isEmpty());
    plugin.getAttributes().put("path", "/admin");
    assertEquals("/admin", plugin.getAttributes().get("path"));
}
```

- [x] **Step 2: 编写失败的前端加载契约测试**

通过可运行时测试的加载辅助函数断言工厂收到 `pluginData: plugin`，仅在工厂返回非空对象时加入集合，并在加入后执行：

```js
bus.$emit(Message.PLUGINS_CHANGED)
```

- [x] **Step 3: 运行失败测试**

Run: `./mvnw -pl fw-plus-project/fw-plus-magicapi -Dtest=PluginTest test`，cwd `fw-plus-core`

Run: `npm test -- --test-name-pattern="插件加载"`，cwd `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console`

Expected: Java 因 `getAttributes()` 不存在失败；前端因 `pluginData` 和 `PLUGINS_CHANGED` 不存在失败。

- [x] **Step 4: 实现最小公共契约**

```java
private Map<String, Object> attributes = new HashMap<>();
```

加载器将工厂结果先赋给局部变量，仅当结果为非空对象时 `push` 并发出事件；字符串、数字、函数、`null` 和 `undefined` 均不得注册。保留原 `plugin.loaded` 状态消息。

- [x] **Step 5: 运行 Task 1 测试并提交**

Run: 上述两个测试命令。

Expected: PASS。

Commit: `feat: add plugin runtime metadata and load event`

### Task 2: 通用状态栏与 Toolbar 稳定排序

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/scripts/plugin-extension.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/test/plugin-extension.test.mjs`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-toolbar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/test/host-boundary.contract.test.mjs`

**Interfaces:**
- Produces: `stableSortByOrder(items)`，不修改输入数组。
- Produces: `isValidStatusBarItem(item)`。
- Consumes: Task 1 的 `Message.PLUGINS_CHANGED`。

- [x] **Step 1: 编写排序和校验失败测试**

```js
assert.deepEqual(
  stableSortByOrder([
    { id: 'late' },
    { id: 'b', order: 20 },
    { id: 'a', order: 10 },
    { id: 'b2', order: 20 },
    { id: 'bad', order: '1' }
  ]).map(it => it.id),
  ['a', 'b', 'b2', 'late', 'bad']
)
assert.equal(isValidStatusBarItem({ icon: 'x', title: 'x', displayKey: 'x', onClick() {} }), true)
assert.equal(isValidStatusBarItem({ icon: 'x', title: 'x' }), false)
```

同时编写可执行的宿主行为测试：构造插件项和 header 配置，断言过滤与排序结果；模拟 `PLUGINS_CHANGED` 后插件集合变化，断言可见状态同步刷新。

- [x] **Step 2: 运行 Task 2 失败测试**

Run: `node --test test/plugin-extension.test.mjs`

Expected: FAIL，模块不存在。

- [x] **Step 3: 实现稳定排序工具**

```js
export const stableSortByOrder = items => items
  .map((item, index) => ({ item, index, order: Number.isFinite(item.order) ? item.order : null }))
  .sort((a, b) => {
    if (a.order === null && b.order === null) return a.index - b.index
    if (a.order === null) return 1
    if (b.order === null) return -1
    return a.order - b.order || a.index - b.index
  })
  .map(entry => entry.item)
```

- [x] **Step 4: 接入状态栏和静态插件 toolbar**

状态栏收集后先校验、按 `displayKey` 过滤、稳定排序，再 `array.splice(0, 0, ...items)`。将收集与事件刷新提取为可执行测试的通用函数；挂载时刷新一次，并订阅 `Message.PLUGINS_CHANGED`。

Toolbar 只将 `constants.PLUGINS[*].toolbars` 扁平化、执行 `adminOnly` 过滤和稳定排序后追加到内置数组；动态事件处理保持原样。

- [x] **Step 5: 运行宿主测试与构建并提交**

Run: `npm test`

Run: `npm run build`

Expected: PASS，构建产出非空。

Commit: `refactor: sort plugin status bar and toolbar extensions`

### Task 3: 迁移现有 Boot 状态栏插件

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-config/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-freemarker/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-dtp/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mqtt/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-vue3/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-satoken/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-kafka/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-pulsar/src/console/src/index.js`
- Modify: 对应模块的 `src/console/src/assets/icon.js`（仅在缺少状态栏 symbol 时）
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/test/plugin-status-bar.contract.test.mjs`
- Modify: S3、Kafka、Pulsar 现有 `src/console/test/*.test.cjs`

**Interfaces:**
- Consumes: Task 2 的 `statusBarItems` 契约。
- Produces: 11 个 Boot 插件状态栏项。

- [x] **Step 1: 编写 Task 3 全量失败契约测试**

用表驱动读取每个 `index.js`，断言 `displayKey`、URL/站内动作和顺序：

```js
const expectedOrders = {
  s3: 100, kafka: 200, pulsar: 300, config: 400, freemarker: 500,
  dtp: 600, debezium: 700, ws: 900, mqtt: 1000, vue3: 1300, satoken: 1400
}
```

外链必须保持设计文档列出的现有地址；WebSocket/MQTT 的 URL 计算整体迁入各自插件。

- [x] **Step 2: 运行 Task 3 测试确认失败**

Run: `node --test test/plugin-status-bar.contract.test.mjs`，cwd 宿主 console。

Expected: FAIL，至少 Config 等插件缺少 `statusBarItems` 和 `order`。

- [x] **Step 3: 在各插件入口增加状态栏项**

统一形态：

```js
statusBarItems: [{
  icon: '#magic-<plugin>-<symbol>',
  title: '<原标题>',
  displayKey: '<原 displayKey>',
  order: <expectedOrders 中的值>,
  onClick: () => <原点击行为>
}]
```

- [x] **Step 4: 运行契约测试和 11 个 Vite 构建**

Run: 宿主 `node --test test/plugin-status-bar.contract.test.mjs`。

Run: 在每个受影响插件 `src/console` 执行 `npm run build`，并在任务证据目录保留命令、cwd、提交、退出码、日志与产物 SHA-256。

Expected: 全部 PASS，IIFE 包含对应 symbol 和 `statusBarItems`。

- [x] **Step 5: 提交 Task 3**

Commit: `feat: move boot plugin status items into plugins`

### Task 4: 恢复 LangChain4j Console 构建来源

**Files:**
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml`
- Modify: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/pom.xml`
- Create: `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-langchain4j/src/console/package.json`
- Create: 同目录 `vite.config.js`、`src/index.js`、`src/assets/icon.js`、`src/components/magic-langchain4j-datasource-info.vue`、`test/fixtures/legacy-contract.json`、`test/langchain4j-plugin.test.mjs`
- Replace generated: `src/main/resources/magic-editor/plugins/magic-langchain4j.1.0.0.iife.js`

**Interfaces:**
- Produces: 全局工厂 `MagicLangchain4j`。
- Produces: `langchain4j` 数据源扩展和 `order: 800` 的状态栏项。

- [x] **Step 1: 从现有 IIFE 固化 Task 4 失败契约**

测试必须先锁定现有插件名、数据源类型、组件注册和文件名，并从旧 Git 对象生成可提交的归一化行为快照，覆盖完整 locale、表单 render、service 成功/失败分支、scoped 样式和图标，再要求新源码包含：

```js
statusBarItems: [{
  icon: '#magic-langchain4j-langchain4j',
  title: 'Langchain4j',
  displayKey: 'langchain4j',
  order: 800,
  onClick: () => window.open('https://docs.langchain4j.dev/', '_blank')
}]
```

- [x] **Step 2: 运行 Task 4 失败测试**

Run: `node --test test/langchain4j-plugin.test.mjs`

Expected: FAIL，`src/console` 构建来源不存在。

- [x] **Step 3: 按现有 IIFE 行为恢复源码和 Vite 配置**

Vite 输出固定为 `MagicLangchain4j` / `magic-langchain4j.1.0.0.iife.js`。POM 使用现有 Boot 插件相同的 npm build 与资源复制生命周期。

- [x] **Step 4: 运行 Task 4 测试、构建和产物对照**

Run: `npm test`，并保留绑定提交的 RED/GREEN 原始日志。

Run: `npm run build`

Run: 使用系统 Maven 对直接子模块执行 `prepare-package`（wrapper 缺失时），保留 effective-POM、生命周期、资源复制、产物大小/SHA-256/`cmp` 的原始证据。

Expected: 测试 PASS；新 IIFE 同时包含原数据源类型、SVG symbol 和状态栏项。

- [x] **Step 5: 提交 Task 4**

Commit: `feat: restore langchain4j plugin console source`

### Task 5: 创建 Druid、Admin、Swagger、Eureka 前端插件

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-druid/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-admin/pom.xml`
- Create: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin-eureka/pom.xml`
- Complete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-swagger/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-druid/src/console/` 下的 `package.json`、`vite.config.js`、`src/index.js`、`src/assets/icon.js`、`test/status-bar.test.mjs`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-admin/src/console/` 下的 `package.json`、`vite.config.js`、`src/index.js`、`src/assets/icon.js`、`test/status-bar.test.mjs`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-swagger/src/console/` 下的 `package.json`、`vite.config.js`、`src/index.js`、`src/assets/icon.js`、`test/status-bar.test.mjs`
- Create: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/fw-plus-magicapi-plugin-eureka/src/console/` 下的 `package.json`、`vite.config.js`、`src/index.js`、`src/assets/icon.js`、`test/status-bar.test.mjs`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-starters/pom.xml`
- Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-dependencies/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/druid/starter/MagicAPIDruidConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/admin/server/MagicAPIAdminServerConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/swagger/MagicSwaggerConfiguration.java`
- Modify: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure/src/main/java/com/xiongx9527/fw/plus/cloud/autoconfigure/magicapi/plugin/eureka/starter/MagicAPIEurekaConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/PluginFrontendMetadataTest.java`
- Create: `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure/src/test/java/com/xiongx9527/fw/plus/cloud/autoconfigure/magicapi/plugin/eureka/starter/MagicAPIEurekaConfigurationTest.java`

**Interfaces:**
- Produces: `MagicDruid`、`MagicAdmin`、`MagicSwagger`、`MagicEureka`。
- Consumes: Task 1 的 `pluginData.attributes.path`。
- Produces: 状态栏顺序 Druid 1100、Swagger 1200、Admin 1500、Eureka 1600。

- [x] **Step 1: 编写四个前端失败测试和 Java 元数据失败测试**

Admin/Eureka 前端测试必须覆盖默认路径与：

```js
const pluginData = { attributes: { path: '/custom-console' } }
```

Java 测试断言 `globalName`、`javascriptFilename` 和 Admin/Eureka 的 `attributes.path`，不允许继续把路径写入 `globalName`。

四个前端测试还必须使用非默认 `MAGIC_API_BASE_URL` 构建 IIFE，并执行产物验证真实点击目标；Druid/Swagger 测试必须锁定迁移前宿主 SVG 的 `viewBox` 和归一化内容或哈希。

- [x] **Step 2: 运行失败测试**

Run: 四个模块的 `npm test`。

Run: `mvn -Dtest=PluginFrontendMetadataTest test`，cwd `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure`。

Run: `mvn -Dtest=MagicAPIEurekaConfigurationTest test`，cwd `fw-plus-cloud/fw-plus-cloud-project/fw-plus-cloud-autoconfigure`。

Expected: FAIL，模块/IIFE 元数据尚不存在。

- [x] **Step 3: 创建最小插件和后端加载配置**

Druid 打开 `/druid/login.html`，Swagger 打开 `/doc.html`；Admin/Eureka 从 `pluginData.attributes.path` 读取动态路径并沿用宿主原 `replaceURL`/基址规则。四个 Vite 配置显式注入 `MAGIC_API_BASE_URL`。Druid/Swagger 从宿主原 SVG 无损生成 symbol，不改变 `viewBox`、path 或视觉内容。每个插件只返回 `name` 和 `statusBarItems`，不新增 toolbar。

- [x] **Step 4: 更新聚合 POM、BOM 和资源装配**

Swagger 使用现有 Boot 占位模块；Druid、Admin 加入 Boot 插件聚合和 Boot BOM。Eureka 模块移动到 Cloud starters，依赖 `fw-plus-eureka-server-spring-cloud-starter`，加入 Cloud starters 聚合和 Cloud BOM。Boot 插件聚合和 Boot BOM 删除 Eureka 模块/依赖，禁止 Boot 对 Cloud 形成反向发布前置。四个模块启用所属层父 POM 的 npm/Vite 构建和资源复制插件。

- [x] **Step 5: 运行前后端测试、四个构建和资源检查**

Run: 四个 `npm test && npm run build`。

Run: Boot/Cloud 目标 Maven 测试。

Run: 检查 Boot reactor 依赖图不含 Cloud artifact，并按 `Core -> Boot -> Cloud` 分层执行目标资源装配。

Expected: 全部 PASS，四个 IIFE 文件存在且名称与 Java 元数据一致；非默认基址产物行为通过；Druid/Swagger SVG 等价；Eureka 仅由 Cloud 聚合/BOM 管理。

- [x] **Step 6: 提交**

Commit: `feat: add status plugins for platform integrations`

### Task 6: 为所有静态插件 Toolbar 增加顺序

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-config/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-db/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-freemarker/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-kafka/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mqttclient/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-pulsar/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-rabbitmq/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-rocketmq/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-satoken/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-vue3/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-word/src/console/src/index.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws/src/console/src/index.js`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/test/plugin-toolbar-order.contract.test.mjs`

**Interfaces:**
- Consumes: Task 2 的静态 toolbar 排序。
- Produces: 所有现有静态插件 toolbar 的有限数值 `order`。

- [x] **Step 1: 编写全量失败契约测试**

表驱动读取 14 个插件 `index.js`，逐一收集 `toolbars` 项并断言每项存在有限数值 `order`。基准顺序使用 100 间隔；同插件多个 toolbar 使用 10 间隔。

- [x] **Step 2: 运行测试确认失败**

Run: `node --test test/plugin-toolbar-order.contract.test.mjs`

Expected: FAIL，现有 toolbar 缺少 `order`。

- [x] **Step 3: 补充明确顺序**

只增加 `order`，不得修改 `id`、`type`、`component`、`adminOnly`、`fullscreen`、`allowClose` 或动态打开逻辑。

- [x] **Step 4: 运行排序、宿主和插件契约测试**

Run: 宿主 `npm test`。

Run: 已有可运行测试的 DB、S3、Kafka、Pulsar 插件 `npm test`。

Expected: 全部 PASS。

- [x] **Step 5: 提交 Task 6**

Commit: `feat: order static plugin toolbars`

### Task 7: 完成宿主专属逻辑清理与边界回归

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/src/components/panel/footer/magic-status-bar.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-spring-boot-starter/src/console/test/host-boundary.contract.test.mjs`

**Interfaces:**
- Consumes: Tasks 1-6 的完整插件扩展契约。
- Produces: 不含任何插件专属名称和行为的宿主状态栏。

- [x] **Step 1: 扩展失败边界测试**

断言宿主不含：

```js
const forbidden = [
  /has[A-Z]\w+Plugin/,
  /checkPlugin/,
  /open(Config|Ftl|Dtp|Langchain4j|Debezium|Satoken|Ws|Mqtt|Druid|Swagger|SpringBootAdmin|Eureka|Vue3)/,
  /加载完毕|Loaded/,
  /setInterval\(/
]
```

同时断言仍存在 `array.splice(0, 0, ...items)`、`config.header[it.displayKey]` 和 `Message.PLUGINS_CHANGED`。

- [x] **Step 2: 运行测试确认失败**

Run: `npm test -- --test-name-pattern="宿主"`

Expected: FAIL，旧专属逻辑仍存在。

- [x] **Step 3: 删除全部专属状态、图标、函数、文案判断和轮询**

删除不再使用的 import；保留固定 Gitee、GitHub、QQ群、帮助和退出入口。

- [x] **Step 4: 运行宿主全量测试和构建**

Run: `npm test`

Run: `npm run build`

Expected: PASS。

- [x] **Step 5: 提交 Task 7**

Commit: `refactor: remove plugin-specific status bar logic`

### Task 8: 跨层集成验证和文档同步

**Files:**
- Modify: `openspec/changes/move-status-bar-items-to-plugins/tasks.md`
- Modify only if implementation discovery requires a small patch: `openspec/changes/move-status-bar-items-to-plugins/specs/magic-plugin-status-bar-extension/spec.md`

**Interfaces:**
- Consumes: Tasks 1-7 的全部实现。
- Produces: 可审计的构建、浏览器和规格验证证据。

- [x] **Step 1: 运行所有受影响前端契约测试与构建**

Run: 宿主及受影响插件 console 的 `npm test`（仅存在可运行测试脚本的模块）。

Run: 全部受影响 console 的 `npm run build`。

Expected: 全部 PASS；每个目标 IIFE 非空。

- [x] **Step 2: 运行 Core、Boot、Cloud、AI 目标 Maven 测试和资源打包**

按依赖顺序使用各顶层 `./mvnw`。若仓库 wrapper 文件缺失或损坏，记录事实后使用系统 `mvn` 执行同一目标命令。

Expected: Java 测试 PASS，四个新增插件和 LangChain4j 的 IIFE 位于目标 JAR 的 `magic-editor/plugins/`。

- [x] **Step 3: 浏览器验证**

启动示例应用和 console，验证：在线人数之后依次显示 16 个插件入口；内置入口位置不变；隐藏配置有效；Admin/Eureka 自定义路径有效；静态 plugin toolbar 顺序稳定；动态 toolbar 点击行为不变；页面无错误文本和图标缺失。

- [x] **Step 4: 运行规格和差异检查**

Run: `openspec validate move-status-bar-items-to-plugins --strict`

Run: `git diff --check`

Expected: 全部 PASS。

- [x] **Step 5: 勾选 OpenSpec 任务并提交**

只有对应验证完成后才勾选 `tasks.md`。提交消息：`test: verify plugin status and toolbar extensions`。
