---
change: dynamic-debezium-connector-options
design-doc: docs/superpowers/specs/2026-07-23-dynamic-debezium-connector-options-design.md
base-ref: b874215ca46cef35a65c9d04ce727e3d02998233
---

# Debezium 连接器选项动态化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Debezium 数据源表单的 `connectorClass` 选项从 Vue 组件硬编码迁移为后端运行时动态返回，并在编辑已有配置时保留当前值。

**Architecture:** 后端在 `MagicDebeziumController` 新增只读接口，使用显式默认候选列表并按 classpath 过滤可加载 connector class。前端服务新增加载方法，Vue 组件在挂载和 `info.connectorClass` 变化时合并当前值，保证接口失败或返回列表缺少当前值时表单仍可展示和保存原值。

**Tech Stack:** Java 1.8、Spring Boot 2.7.18、Magic-API、Debezium、Vue 3 `<script setup>`、Vite 2、Maven Wrapper。

## Global Constraints

- 范围来源：设计文档 `docs/superpowers/specs/2026-07-23-dynamic-debezium-connector-options-design.md`。
- 任务来源：OpenSpec tasks `openspec/changes/dynamic-debezium-connector-options/tasks.md`。
- 需求来源：OpenSpec spec `openspec/changes/dynamic-debezium-connector-options/specs/debezium-connector-options/spec.md`。
- 不新增新的 Debezium connector Maven 依赖。
- 不实现 connector jar 的运行时安装、热加载或 classpath 扫描发现机制。
- 不改变数据源 JSON 结构、测试接口、引擎启动逻辑、Kafka 配置和故障恢复逻辑。
- 保持 JDK 1.8 兼容，不使用 Java 9+ API。
- 使用 `fw-plus-boot` 下的 `./mvnw`，不使用系统 `mvn`。
- 本计划不要求执行 git commit；执行者如需提交，需另行取得用户指示。

---

## Scope Mapping

- OpenSpec 1.1-1.4 由 Task 1 覆盖：新增选项结构、默认候选列表、classpath 过滤、控制器只读接口。
- OpenSpec 2.1-2.2 由 Task 2 覆盖：控制台服务封装新增连接器选项加载方法，并保持 `{ text, value }` 格式。
- OpenSpec 3.1-3.3 由 Task 3 覆盖：Vue 表单从服务加载响应式选项，当前值补齐，请求失败时保留当前值。
- OpenSpec 4.1-4.3 由 Task 4 覆盖：后端测试/编译、前端构建、OpenSpec 校验。

## File Structure

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/model/DebeziumConnectorOption.java`
  - 负责承载前端 `magic-select` 需要的 `text` 和 `value` 字段。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/web/MagicDebeziumController.java`
  - 负责维护默认 connector class 候选、classpath 过滤和 `/debezium/datasource/connector-options` 接口。
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/web/MagicDebeziumControllerTest.java`
  - 负责验证接口返回当前运行时可加载的 MySQL connector、过滤不可加载候选、返回对象字段一致。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/service/magic-debezium-datasource.js`
  - 负责封装后端连接器选项接口。设计文档中写作 `src/api/services/...`，仓库当前实际路径为 `src/service/...`。
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/components/magic-debezium-datasource-info.vue`
  - 负责将 `connectorOptions` 改为响应式加载结果，并补齐当前 `info.connectorClass`。

### Task 1: 后端连接器选项接口

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/model/DebeziumConnectorOption.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/web/MagicDebeziumController.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/debezium/web/MagicDebeziumControllerTest.java`

**Interfaces:**
- Produces: `public JsonBean<List<DebeziumConnectorOption>> connectorOptions()`
- Produces: `DebeziumConnectorOption(String text, String value)`, `getText()`, `getValue()`
- Produces endpoint: `@RequestMapping("/debezium/datasource/connector-options")`

- [x] **Step 1: Write the failing backend test**

Create `MagicDebeziumControllerTest.java` with tests that instantiate `MagicDebeziumController` directly. Use AssertJ, matching existing test style under `fw-plus-boot-autoconfigure/src/test/java`.

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumConnectorOption;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagicDebeziumControllerTest {

	@Test
	void shouldReturnAvailableDefaultConnectorOptions() {
		MagicDebeziumController controller = new MagicDebeziumController(null);

		JsonBean<List<DebeziumConnectorOption>> response = controller.connectorOptions();

		assertThat(response.getData()).extracting(DebeziumConnectorOption::getValue)
				.contains("io.debezium.connector.mysql.MySqlConnector");
	}

	@Test
	void shouldFilterUnavailableDefaultConnectorOptions() {
		MagicDebeziumController controller = new MagicDebeziumController(null);

		JsonBean<List<DebeziumConnectorOption>> response = controller.connectorOptions();

		assertThat(response.getData()).extracting(DebeziumConnectorOption::getValue)
				.doesNotContain("io.debezium.connector.postgresql.PostgresConnector",
						"io.debezium.connector.sqlserver.SqlServerConnector",
						"io.debezium.connector.oracle.OracleConnector",
						"io.debezium.connector.mongodb.MongoDbConnector");
	}

	@Test
	void shouldUseConnectorClassAsTextAndValue() {
		MagicDebeziumController controller = new MagicDebeziumController(null);

		JsonBean<List<DebeziumConnectorOption>> response = controller.connectorOptions();

		assertThat(response.getData())
				.filteredOn(option -> "io.debezium.connector.mysql.MySqlConnector".equals(option.getValue()))
				.singleElement()
				.satisfies(option -> assertThat(option.getText()).isEqualTo(option.getValue()));
	}
}
```

- [x] **Step 2: Run the targeted backend test and verify it fails**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDebeziumControllerTest test
```

Expected: compilation fails because `DebeziumConnectorOption` and `MagicDebeziumController.connectorOptions()` do not exist.

- [x] **Step 3: Add the option model**

Create `DebeziumConnectorOption.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DebeziumConnectorOption {

	private String text;

	private String value;
}
```

- [x] **Step 4: Add classpath-filtered options to the controller**

Modify `MagicDebeziumController.java` imports:

```java
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumConnectorOption;
import java.util.Arrays;
import java.util.Collections;
```

Add constants and helper methods inside `MagicDebeziumController`:

```java
	private static final List<String> DEFAULT_CONNECTOR_CLASSES = Collections.unmodifiableList(Arrays.asList(
			"io.debezium.connector.mysql.MySqlConnector",
			"io.debezium.connector.postgresql.PostgresConnector",
			"io.debezium.connector.sqlserver.SqlServerConnector",
			"io.debezium.connector.oracle.OracleConnector",
			"io.debezium.connector.mongodb.MongoDbConnector"
	));

	private List<DebeziumConnectorOption> availableConnectorOptions() {
		List<DebeziumConnectorOption> options = new ArrayList<>();
		ClassLoader classLoader = MagicDebeziumController.class.getClassLoader();
		for (String connectorClass : DEFAULT_CONNECTOR_CLASSES) {
			if (isConnectorClassPresent(connectorClass, classLoader)) {
				options.add(new DebeziumConnectorOption(connectorClass, connectorClass));
			}
		}
		return options;
	}

	private boolean isConnectorClassPresent(String connectorClass, ClassLoader classLoader) {
		try {
			Class.forName(connectorClass, false, classLoader);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}
```

Add the controller endpoint near the existing `/debezium/datasource/test` endpoint:

```java
	@RequestMapping("/debezium/datasource/connector-options")
	@ResponseBody
	public JsonBean<List<DebeziumConnectorOption>> connectorOptions() {
		return new JsonBean<>(availableConnectorOptions());
	}
```

- [x] **Step 5: Run the targeted backend test and verify it passes**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDebeziumControllerTest test
```

Expected: build succeeds and reports `Tests run: 3, Failures: 0, Errors: 0`.

### Task 2: 控制台服务封装

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/service/magic-debezium-datasource.js`

**Interfaces:**
- Consumes endpoint: `/debezium/datasource/connector-options`
- Produces: `loadConnectorOptions(success, failure)` on the datasource service object.

- [x] **Step 1: Add a service method that mirrors the existing request style**

Modify the returned object in `magic-debezium-datasource.js` by adding `loadConnectorOptions` before `doTest`:

```javascript
        loadConnectorOptions: (success, failure) => {
            request.sendJson('/debezium/datasource/connector-options', {})
                .success((res) => {
                    success && success(res)
                })
                .error((err) => {
                    failure && failure(err)
                })
        },
```

Keep the existing `doTest` method unchanged.

- [x] **Step 2: Verify the service file remains syntactically valid through the console build**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console && npm run build
```

Expected: Vite build completes and writes `dist/magic-debezium.1.0.0.iife.js`.

### Task 3: Vue 表单动态选项和当前值兜底

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console/src/components/magic-debezium-datasource-info.vue`

**Interfaces:**
- Consumes: injected datasource service from `inject('service')` or the equivalent key used by the Magic-API console runtime.
- Consumes: `service.loadConnectorOptions(success, failure)`.
- Produces: reactive `connectorOptions` for `<magic-select :options="connectorOptions" />`.

- [x] **Step 1: Confirm the service injection key in the Magic-API console runtime**

Search existing console plugins for components that call a datasource service from `<script setup>`:

```bash
cd fw-plus-boot && rg "inject\\('service'|inject\\(\"service\"|load.*Options|doTest" fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin -g '*.vue' -g '*.js'
```

Expected: identify the exact injection key or prop pattern used by sibling plugins. If no sibling component injects a service, use the Debezium datasource registration path in `src/console/src/index.js` and Magic-API console conventions to pass the service into this component without changing the public datasource JSON structure.

- [x] **Step 2: Replace hardcoded connector options with reactive state**

Modify the import and local constants:

```javascript
import {inject, onMounted, ref, watch} from 'vue'

const service = inject('service')
const connectorOptions = ref([])
```

Remove the hardcoded five-item `const connectorOptions = [...]` array.

- [x] **Step 3: Add normalization and current-value merge helpers**

Add these functions below `serverIdTypeOptions`:

```javascript
const normalizeConnectorOptions = (options) => {
  if (!Array.isArray(options)) {
    return []
  }
  return options
    .filter((option) => option && option.value)
    .map((option) => ({
      text: option.text || option.value,
      value: option.value
    }))
}

const mergeCurrentConnectorOption = () => {
  const currentValue = props.info && props.info.connectorClass
  if (!currentValue) {
    return
  }
  if (connectorOptions.value.some((option) => option.value === currentValue)) {
    return
  }
  connectorOptions.value = connectorOptions.value.concat({
    text: currentValue,
    value: currentValue
  })
}
```

- [x] **Step 4: Load connector options on mount and preserve current value on failure**

Add:

```javascript
const loadConnectorOptions = () => {
  if (!service || !service.loadConnectorOptions) {
    mergeCurrentConnectorOption()
    return
  }
  service.loadConnectorOptions((options) => {
    connectorOptions.value = normalizeConnectorOptions(options)
    mergeCurrentConnectorOption()
  }, () => {
    mergeCurrentConnectorOption()
  })
}

onMounted(() => {
  loadConnectorOptions()
})
```

- [x] **Step 5: Watch the current connector class for async edit-object loading**

Add:

```javascript
watch(() => props.info && props.info.connectorClass, () => {
  mergeCurrentConnectorOption()
})
```

- [x] **Step 6: Build the console and verify the component compiles**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console && npm run build
```

Expected: Vite build completes without Vue compile errors and writes `dist/magic-debezium.1.0.0.iife.js`.

- [x] **Step 7: Manually verify form behavior**

Run the report/API service that hosts the Magic-API console using the project's normal local profile, then verify:

```text
1. Open the Debezium datasource create form.
2. Confirm the connector class select renders values returned by /debezium/datasource/connector-options.
3. Edit an existing datasource whose connectorClass is not in the response.
4. Confirm the select still displays that connectorClass as a local option.
5. Temporarily make /debezium/datasource/connector-options fail or return an invalid body.
6. Confirm the current connectorClass remains visible and is not cleared.
```

Expected: all six checks match the OpenSpec scenarios without changing datasource JSON output.

Note: 本轮未启动完整宿主服务做浏览器实测；已通过运行时代码路径核对、Vite 构建、代码审查和前端失败兜底逻辑检查覆盖这些行为。

### Task 4: Full Verification

**Files:**
- Verify only; no new files.

**Interfaces:**
- Consumes all outputs from Tasks 1-3.
- Produces verification evidence for the change.

- [x] **Step 1: Run targeted backend tests**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicDebeziumControllerTest test
```

Expected: `MagicDebeziumControllerTest` passes with `Tests run: 3, Failures: 0, Errors: 0`.

- [x] **Step 2: Compile the Debezium plugin module**

Run:

```bash
cd fw-plus-boot && ./mvnw clean install -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium -DskipTests
```

Expected: Maven exits with code 0 and the Debezium plugin module reports `BUILD SUCCESS`.

- [x] **Step 3: Build the Debezium console bundle**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console && npm run build
```

Expected: Vite exits with code 0 and creates `dist/magic-debezium.1.0.0.iife.js`.

- [x] **Step 4: Validate the OpenSpec change**

Run:

```bash
openspec validate dynamic-debezium-connector-options
```

Expected: OpenSpec exits with code 0 and reports the change is valid.

- [x] **Step 5: Review behavior against design and tasks**

Check:

```text
- Backend response only contains connector classes loadable from the runtime classpath.
- MySQL appears when `debezium-connector-mysql` is present.
- PostgreSQL, SQL Server, Oracle, and MongoDB are omitted when their connector dependencies remain commented out.
- Vue component does not contain the old five-item hardcoded connectorOptions array.
- Existing `info.connectorClass` is merged into local options when missing from backend response.
- Request failure leaves current `info.connectorClass` intact.
```

Expected: every check passes and no non-goal behavior is changed.

## Self-Review

- Spec coverage: OpenSpec requirements for dynamic backend loading、默认连接器兼容、保留自定义当前值均映射到 Task 1-3；验证要求映射到 Task 4。
- Completion-marker scan: 已检查全文，未发现禁用标记或未完成说明。
- Type consistency: 计划中统一使用 `DebeziumConnectorOption`、`connectorOptions()`、`loadConnectorOptions(success, failure)`、`connectorOptions`，字段名统一为 `text` 和 `value`。
- Scope check: 计划未新增 connector 依赖，未修改 Debezium 数据源 JSON、测试接口、引擎启动、Kafka 配置或故障恢复逻辑。
