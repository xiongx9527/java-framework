---
change: dynamic-s3-platform-options
design-doc: docs/superpowers/specs/2026-07-23-dynamic-s3-platform-options-design.md
base-ref: 64abaf80da3a779425b9046cc936c3f11766f0e3
archived-with: 2026-07-23-dynamic-s3-platform-options
---

# Dynamic S3 Platform Options Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Magic-API S3 datasource 表单的平台下拉登录后按运行时可用能力动态展示，同时保留旧前端兜底和现有 datasource 保存格式。

**Architecture:** 后端新增 S3 平台候选模型、配置属性和 Provider，Provider 按配置类 classpath、实际 SDK 类 classpath 与 `MagicDynamicS3DataSource` 支持表过滤平台。`MagicS3Controller` 暴露登录后接口，前端组件初始化后加载接口候选，接口失败或空结果时保留默认候选，并把当前 datasource 的历史 `platform` 临时追加到候选中。

**Tech Stack:** Java 8、Spring Boot 2.7.18、Magic-API、JUnit 5、Mockito、Vue 3 `<script setup>`、Vite。

## Global Constraints

- 所有 Java 实现必须兼容 Java 8，不使用 `List.of()`、`Map.of()`、`var` 等 Java 9+ API。
- 不改变 S3 datasource JSON 保存结构，继续使用现有 `platform` 字段。
- 动态接口必须登录后访问，并复用 Magic-API 控制台 VIEW 权限校验。
- 动态候选只展示 classpath 存在配置类、实际 SDK 类且后端明确可构建的平台。
- 旧 `aws-s3` 只作为前端兜底和历史值保留，不作为后端动态默认候选。

---

## File Structure

- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOption.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformCandidate.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOptionProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOptionProvider.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/starter/MagicAPIS3Configuration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/web/MagicS3Controller.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOptionProviderTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/web/MagicS3ControllerPlatformOptionsTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-datasource-info.vue`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/README.md`
- Modify: `openspec/changes/dynamic-s3-platform-options/tasks.md`

### Task 1: 后端 Provider 测试与实现

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOptionProviderTest.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOption.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformCandidate.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOptionProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/options/MagicS3PlatformOptionProvider.java`

**Interfaces:**
- Produces: `MagicS3PlatformOptionProvider#getOptions(): List<MagicS3PlatformOption>`
- Produces: `MagicS3PlatformOption#getText(): String`
- Produces: `MagicS3PlatformOption#getValue(): String`
- Consumes: `SpringFileStorageProperties` nested config class names as default classpath probes.

- [x] **Step 1: Write the failing Provider tests**

Create `MagicS3PlatformOptionProviderTest` with tests:

```java
@Test
void shouldReturnSupportedDefaultPlatformsWhenConfigClassesExist() {
    MagicS3PlatformOptionProvider provider = new MagicS3PlatformOptionProvider(
            new MagicS3PlatformOptionProperties(),
            MagicS3PlatformOptionProviderTest.class.getClassLoader());

    List<MagicS3PlatformOption> options = provider.getOptions();

    assertTrue(containsValue(options, "minio"));
    assertTrue(containsValue(options, "aliyun-oss"));
    assertTrue(containsValue(options, "tencent-cos"));
    assertTrue(containsValue(options, "amazon-s3"));
    assertFalse(containsValue(options, "aws-s3"));
}
```

Also add tests for missing class filtering, configured available candidate, configured unsupported candidate, and stable de-duplication.

- [x] **Step 2: Run Provider tests to verify RED**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicS3PlatformOptionProviderTest test
```

Expected: compilation fails because `MagicS3PlatformOptionProvider` and related classes do not exist.

- [x] **Step 3: Implement Provider classes minimally**

Implement:

- `MagicS3PlatformOption`: JavaBean with `text` and `value`, no Lombok required unless nearby style prefers it.
- `MagicS3PlatformCandidate`: JavaBean with `text`, `value`, `configClassName`, `requiredClassNames`.
- `MagicS3PlatformOptionProperties`: JavaBean with `List<MagicS3PlatformCandidate> platformCandidates`.
- `MagicS3PlatformOptionProvider`: default candidates, config class and SDK class classpath check using `ClassUtils.isPresent`, backend support set, de-duplicate by `value`.

- [x] **Step 4: Run Provider tests to verify GREEN**

Run the same Maven command. Expected: `MagicS3PlatformOptionProviderTest` passes.

### Task 2: S3 Controller 登录接口与自动配置

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/web/MagicS3ControllerPlatformOptionsTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/web/MagicS3Controller.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/s3/starter/MagicAPIS3Configuration.java`

**Interfaces:**
- Consumes: `MagicS3PlatformOptionProvider#getOptions()`
- Produces: `GET ${magic-api.web:/magic-api/web}/s3/datasource/platform/options`
- Produces: `JsonBean<List<MagicS3PlatformOption>>`

- [x] **Step 1: Write the failing Controller tests**

Create tests that:

- Reflect `MagicS3Controller#platformOptions(MagicHttpServletRequest)`.
- Assert `@GetMapping("${magic-api.web:/magic-api/web}/s3/datasource/platform/options")`.
- Assert `@Valid(requireLogin = true)`.
- Assert generic return type is `JsonBean<List<MagicS3PlatformOption>>`.
- Configure `MagicConfiguration#setAuthorizationInterceptor` to reject `Authorization.VIEW` and assert `platformOptions(...)` throws.

- [x] **Step 2: Run Controller tests to verify RED**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicS3ControllerPlatformOptionsTest test
```

Expected: compilation fails because `platformOptions` endpoint does not exist.

- [x] **Step 3: Implement Controller endpoint and Bean registration**

Modify `MagicS3Controller`:

- Add constructor `MagicS3Controller(MagicConfiguration, MagicS3PlatformOptionProvider)`.
- Keep existing constructor delegating to a default Provider for compatibility.
- Add `platformOptions(MagicHttpServletRequest request)` with `@Valid(requireLogin = true)`.
- Call `allowVisit(request, Authorization.VIEW)` and return provider options.

Modify `MagicAPIS3Configuration`:

- Add `@EnableConfigurationProperties(MagicS3PlatformOptionProperties.class)` if needed.
- Add `MagicS3PlatformOptionProvider` bean.
- Pass provider into `new MagicS3Controller(configuration, provider)` in `controllerRegister()`.

- [x] **Step 4: Run Controller and Provider tests to verify GREEN**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicS3PlatformOptionProviderTest,MagicS3ControllerPlatformOptionsTest test
```

Expected: both test classes pass.

### Task 3: 前端动态加载与旧值兼容

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console/src/components/magic-s3-datasource-info.vue`

**Interfaces:**
- Consumes: `request.sendGet('/s3/datasource/platform/options')`
- Consumes: response array items `{ text: string, value: string }`
- Produces: `platformOptions` reactive array for `magic-select`

- [x] **Step 1: Add the dynamic loading implementation**

Modify `<script setup>`:

- Import `onMounted`, `ref`, `watch`.
- Import or inject existing console `request` utility following local project pattern.
- Rename static list to `defaultPlatformOptions`.
- Create `const platformOptions = ref(defaultPlatformOptions.slice())`.
- Add `normalizeOptions(options)` to accept only items with non-empty `value`.
- Add `ensureCurrentPlatformOption()` to append current `props.info.platform` when absent.
- Load `/s3/datasource/platform/options` on mount.
- Keep defaults on error or empty response.

- [x] **Step 2: Build S3 console**

Run:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console && npm run build
```

Expected: Vite build succeeds and emits `dist/magic-s3.1.0.0.iife.js`.

- [x] **Step 3: Fix frontend build errors if any**

If build fails due to request import path, inspect neighboring plugin console components and adjust import/injection to the existing request pattern. Re-run `npm run build` until it passes.

### Task 4: 使用说明与任务勾选

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/README.md`
- Modify: `openspec/changes/dynamic-s3-platform-options/tasks.md`

**Interfaces:**
- Documents: default candidate behavior, dynamic endpoint, login requirement, config extension, `aws-s3` compatibility.

- [x] **Step 1: Write S3 plugin README**

Create README with sections:

- S3 datasource platform dropdown is loaded from backend after Magic-API console login.
- Endpoint: `GET ${magic-api.web:/magic-api/web}/s3/datasource/platform/options`.
- Returned item shape: `{ "text": "minio", "value": "minio" }`.
- Default dynamic backend values include `minio`, `aliyun-oss`, `tencent-cos`, `amazon-s3`, `huawei-obs`, `qiniu-kodo`, `baidu-bos`, `upyun-uss` when corresponding classes exist.
- Frontend fallback keeps old options, including `aws-s3`, only when endpoint is unavailable or editing an old value.

- [x] **Step 2: Run final backend and frontend checks**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicS3PlatformOptionProviderTest,MagicS3ControllerPlatformOptionsTest test
```

Then:

```bash
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-s3/src/console && npm run build
```

Expected: both commands pass.

- [x] **Step 3: Update OpenSpec task checklist**

Mark completed items in `openspec/changes/dynamic-s3-platform-options/tasks.md` only after tests/build pass. Keep unchecked any item whose verification could not run, and write the reason in the final report.
